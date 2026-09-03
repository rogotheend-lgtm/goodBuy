# goodBuy Spring Backend

소비 내역 이미지를 한 번 요청받아 Python OCR 결과를 검증하고, 소비/이체/이상 항목으로 분류한 결과를 저장하지 않고 프론트엔드에 바로 반환하는 Spring Boot API입니다.

## 전체 서비스 한 번에 실행

프로젝트 루트에서 Python OCR, Spring Backend, Vue Frontend를 함께 실행하거나 종료합니다.

```bash
cd /Users/lim/goodBuy
./goodbuy.sh start   # 전체 실행
./goodbuy.sh stop    # 전체 종료
```

필요하면 다음 명령도 사용할 수 있습니다.

```bash
./goodbuy.sh restart # 전체 재시작
./goodbuy.sh status  # 실행 상태 확인
./goodbuy.sh logs    # 세 서비스 로그 실시간 확인 (종료: Ctrl+C)
```

## 처리 흐름

1. FE가 같은 `images` 필드로 이미지 1~5장과 `ownerName`을 multipart 요청으로 전송합니다.
2. Spring이 각 이미지의 형식과 크기를 검사합니다.
3. Spring이 Python OCR에 이미지를 한 장씩 순서대로 전달하고, 각 응답 스키마와 합계를 검증합니다.
4. OCR이 모두 끝나면 Spring이 Supabase의 `categories`와 `category_rules`를 한 번의 읽기 전용 JOIN 쿼리로 조회합니다.
5. Spring이 가장 긴 키워드 우선으로 카테고리를 분류하고, 이상치와 최종 합계를 계산합니다.
6. 최종 분류 결과를 PostgreSQL에 저장하지 않습니다.
7. 같은 요청의 응답으로 반환한 뒤 서버에 분석 기록을 남기지 않습니다.

사용자 정보, 이미지, 분석 결과와 세션을 저장하지 않으며 분석 ID도 만들지 않습니다. 별도 조회나 수정 API도 없습니다. Supabase의 `categories`, `category_rules`는 서비스 공통 기준 정보로만 읽습니다.

## 핵심 분류 규칙

- 입력한 이름과 거래 대상이 공백/대소문자/유니코드 정규화 후 정확히 같으면 `SELF_TRANSFER` 이상치로 표시하고 소비 합계에서 제외합니다.
- `토스페이`, `카카오페이`, `네이버페이`처럼 결제인지 송금인지 확정할 수 없는 항목은 `ANOMALY`로 표시하고 이유를 출력합니다.
- 거래 금액이 DB 카테고리의 `dutch_threshold`를 초과하면 `GROUP_PAYMENT_CANDIDATE`로 표시하되 소비 합계에는 포함합니다.
- 사용 목적(`purposeCategory`)과 가맹점 형태(`merchantType`)를 분리합니다.
- 이상치는 사용자에게 재입력을 요청하지 않으며 `anomalyDetail`을 통해 탐지 근거만 안내합니다.

현재 목적 카테고리는 `FOOD`, `TRANSPORT`, `LIVING`, `SHOPPING`, `CULTURE_HOBBY`, `HEALTH`, `EDUCATION`, `FIXED_SUBSCRIPTION`, `OTHER`입니다. 키워드와 기준 금액은 Supabase 기준 데이터를 사용하며, DB 함수는 호출하지 않습니다.

## 로컬 실행

Java 21과 PostgreSQL이 필요합니다. 기본 DB는 `localhost:5432/goodbuy`, 사용자와 비밀번호는 모두 `goodbuy`입니다.

```bash
cd /Users/lim/goodBuy/backend-spring
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`을 열면 이미지 업로드, 분석 결과와 이상치 상세 내용을 한 화면에서 테스트할 수 있습니다.

Swagger UI에서는 `http://localhost:8080/swagger.html`을 열어 `POST /api/v1/analyses`를 직접 실행할 수 있습니다. OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 확인할 수 있습니다.

구현된 API만 간단히 정리한 페이지는 `http://localhost:8080/api-summary.html`에서 확인할 수 있습니다.

기본값은 실제 Python 서버 없이도 FE 연동을 진행할 수 있는 `mock` OCR 모드입니다. 실제 Python OCR에 연결할 때는 다음 환경 변수를 사용합니다.

```bash
OCR_MODE=python \
OCR_BASE_URL=http://localhost:8000 \
OCR_PARSE_PATH=/ocr/extraction \
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
| `OCR_READ_TIMEOUT` | `120s` | 실제 OCR 응답 대기 시간 |
| `FRONTEND_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | API 호출을 허용할 FE Origin 목록 |

## API

### 이미지 분석

```http
POST /api/v1/analyses
Content-Type: multipart/form-data

images: PNG 또는 JPEG 1~5장, 파일당 최대 10MB (같은 필드명을 반복)
ownerName: 계좌 소유자 이름, 공백 제외 2~30자
```

```javascript
const form = new FormData();
for (const imageFile of imageFiles) {
  form.append("images", imageFile);
}
form.append("ownerName", ownerName);

const response = await fetch("http://localhost:8080/api/v1/analyses", {
  method: "POST",
  body: form,
});

const analysis = await response.json();
```

이상 거래는 각 거래의 `anomaly`, `anomalyReason`, `anomalyDetail` 필드로 출력됩니다. 별도의 수정 요청 API는 제공하지 않습니다.

## Python OCR 계약

Spring은 Python의 다음 내부 API를 이미지마다 한 번씩 순차 호출합니다. Python API의 단일 이미지 계약은 그대로 유지됩니다.

```http
POST /ocr/extraction
Content-Type: multipart/form-data

file: 업로드 이미지
```

응답 JSON Schema는 `docs/ocr-response.schema.json`에 있습니다. Python의 `summary`는 OCR 파싱 결과 검증용이며, 사용자에게 보여줄 소비 합계는 Spring이 거래 분류 후 다시 계산합니다.

## 테스트

Docker가 실행 중인 환경에서 다음 명령으로 임시 PostgreSQL, OCR 계약, 분류 규칙, 요청 무기록과 이상치 상세 응답을 테스트합니다.

```bash
./gradlew clean test
```
