# goodBuy Spring Backend

익명 사용자가 소비 내역 이미지를 올리면 Python OCR 결과를 검증하고, 소비/이체/확인 필요 항목으로 분류한 뒤 PostgreSQL에 저장하는 Spring Boot API입니다.

## 처리 흐름

1. FE가 같은 `images` 필드로 이미지 1~5장과 `ownerName`을 multipart 요청으로 전송합니다.
2. Spring이 각 이미지의 형식과 크기를 검사하고 익명 세션 쿠키를 발급합니다.
3. Spring이 Python OCR에 이미지를 한 장씩 순서대로 전달하고, 각 응답 스키마와 합계를 검증합니다.
4. 거래를 분류하고 Spring이 신뢰 가능한 최종 합계를 다시 계산합니다.
5. 세션과 분석 결과를 PostgreSQL에 저장합니다.
6. 이후 조회/수정 요청은 같은 익명 세션의 데이터에만 접근할 수 있습니다.

## 핵심 분류 규칙

- 입력한 이름과 거래 대상이 공백/대소문자/유니코드 정규화 후 정확히 같으면 `SELF_TRANSFER`로 분류하고 소비 합계에서 제외합니다.
- `토스페이`, `카카오페이`, `네이버페이`처럼 결제인지 송금인지 확정할 수 없는 항목은 `NEEDS_REVIEW`로 둡니다.
- 카페·패스트푸드·일반 식당·고깃집별 1인 기준 금액의 3배 이상인 단일 거래는 `GROUP_PAYMENT_CANDIDATE`로 확인을 요청합니다.
- 사용 목적(`purposeCategory`)과 가맹점 형태(`merchantType`)를 분리합니다.
- 사용자가 검토를 마친 항목만 `DecisionSource.USER`로 기록합니다.

현재 목적 카테고리는 `FOOD`, `TRANSPORT`, `LIVING`, `SHOPPING`, `CULTURE_HOBBY`, `HEALTH`, `EDUCATION`, `FIXED_SUBSCRIPTION`, `OTHER`입니다. 팀 회의에서 이름이나 범위를 바꾸더라도 enum과 분류 규칙만 함께 변경하면 됩니다.

## 로컬 실행

Java 21과 PostgreSQL이 필요합니다. 기본 DB 설정은 다음과 같습니다.

```text
database: goodbuy
username: goodbuy
password: goodbuy
port: 5432
```

DB가 준비된 뒤 실행합니다.

```bash
cd /Users/lim/goodBuy/backend-spring
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`을 열면 이미지 업로드, 분석 결과 확인, 확인 필요 거래 수정을 한 화면에서 테스트할 수 있습니다.

Swagger UI에서는 `http://localhost:8080/swagger.html`을 열어 동일한 API를 직접 실행할 수 있습니다. 익명 세션 쿠키가 필요하므로 Swagger에서 먼저 `POST /api/v1/analyses`를 실행한 뒤 응답의 `analysisId`와 거래 `id`를 사용해 GET/PATCH를 테스트합니다. OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 확인할 수 있습니다.

구현된 API만 간단히 정리한 페이지는 `http://localhost:8080/api-summary.html`에서 확인할 수 있습니다.

기본값은 실제 Python 서버 없이도 FE 연동을 진행할 수 있는 `mock` OCR 모드입니다. 실제 Python OCR에 연결할 때는 다음 환경 변수를 사용합니다.

```bash
OCR_MODE=python \
OCR_BASE_URL=http://localhost:8000 \
OCR_PARSE_PATH=/internal/v1/ocr/parse \
./gradlew bootRun
```

주요 환경 변수:

| 변수 | 기본값 | 용도 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/goodbuy` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `goodbuy` | DB 사용자 |
| `DB_PASSWORD` | `goodbuy` | DB 비밀번호 |
| `OCR_MODE` | `mock` | `mock` 또는 `python` |
| `OCR_BASE_URL` | `http://localhost:8000` | Python 서버 주소 |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | 쿠키를 허용할 FE Origin |
| `COOKIE_SECURE` | `false` | HTTPS 배포 환경에서는 `true` |

Flyway가 시작 시 `src/main/resources/db/migration`의 스키마를 자동 생성하고 Hibernate는 그 결과를 검증합니다.

## API

### 이미지 분석

```http
POST /api/v1/analyses
Content-Type: multipart/form-data

images: PNG 또는 JPEG 1~5장, 파일당 최대 10MB (같은 필드명을 반복)
ownerName: 계좌 소유자 이름, 공백 제외 2~30자
```

브라우저에서는 세션 쿠키를 주고받을 수 있도록 `credentials: "include"`를 사용해야 합니다.

```javascript
const form = new FormData();
for (const imageFile of imageFiles) {
  form.append("images", imageFile);
}
form.append("ownerName", ownerName);

const response = await fetch("http://localhost:8080/api/v1/analyses", {
  method: "POST",
  credentials: "include",
  body: form,
});
```

### 분석 결과 조회

```http
GET /api/v1/analyses/{analysisId}
Cookie: goodbuy_session=...
```

### 확인 필요 거래 수정

```http
PATCH /api/v1/transactions/{transactionId}
Content-Type: application/json
Cookie: goodbuy_session=...

{
  "transactionType": "EXPENSE",
  "purposeCategory": "FOOD",
  "personalAmount": 630
}
```

`transactionType`은 사용자가 확정할 수 있는 `EXPENSE`, `SELF_TRANSFER`, `OTHER_PERSON` 중 하나입니다. 단체 결제를 나눌 때 `personalAmount`에 본인 부담액을 입력합니다.

## Python OCR 계약

Spring은 Python의 다음 내부 API를 이미지마다 한 번씩 순차 호출합니다. Python API의 단일 이미지 계약은 그대로 유지됩니다.

```http
POST /internal/v1/ocr/parse
Content-Type: multipart/form-data

image: 업로드 이미지
```

응답 JSON Schema는 `docs/ocr-response.schema.json`에 있습니다. Python의 `summary`는 OCR 파싱 결과 검증용이며, 사용자에게 보여줄 소비 합계는 Spring이 거래 분류 후 다시 계산합니다.

## 테스트

Docker가 실행 중인 환경에서 다음 명령을 사용합니다. 통합 테스트는 Testcontainers가 임시 PostgreSQL을 만들고 Flyway, JPA, 익명 세션 격리, API 요청/수정까지 검증합니다.

```bash
./gradlew clean test
```
