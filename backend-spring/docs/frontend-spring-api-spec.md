# Frontend - Spring Backend API 명세서

## 1. 공통 정보

| 항목 | 값 |
| --- | --- |
| 로컬 Spring 주소 | `http://localhost:8080` |
| 로컬 Frontend 주소 | `http://localhost:5173`, `http://127.0.0.1:5173` |
| 요청 형식 | `multipart/form-data` |
| 응답 형식 | `application/json` |
| 인증 | 없음 |
| 결과 저장 및 재조회 | 없음. 분석 결과를 요청 응답으로 한 번만 반환 |

## 2. API 목록

| 기능명 | HTTP Method | Endpoint | Request | Response | 비고 |
| --- | --- | --- | --- | --- | --- |
| 소비내역 이미지 분석 | `POST` | `/api/v1/analyses` | 이름과 이미지 1~5장 | 거래 분류 목록과 최종 요약 | Python OCR 및 DB 카테고리 조회가 끝날 때까지 동기 처리 |

## 3. 소비내역 이미지 분석

### 3.1 요청

```http
POST /api/v1/analyses
Content-Type: multipart/form-data
```

| Part 이름 | 타입 | 필수 | 제한 | 설명 |
| --- | --- | --- | --- | --- |
| `ownerName` | String | O | 공백 제외 2~30자 | 자가 이체 판별에만 사용하며 저장하지 않음 |
| `images` | Binary File[] | O | 1~5장 | 같은 이름의 multipart part를 이미지마다 반복 |

이미지 제한:

- 허용 형식: PNG(`image/png`), JPEG(`image/jpeg`)
- 이미지 한 장당 최대 10MB
- 전체 multipart 요청 최대 50MB
- 선언한 Content-Type과 실제 PNG/JPEG 파일 시그니처가 일치해야 함

Frontend 요청 예시:

```javascript
const formData = new FormData()
formData.append('ownerName', ownerName)
images.forEach((image) => formData.append('images', image))

const response = await fetch('http://localhost:8080/api/v1/analyses', {
  method: 'POST',
  body: formData,
})
```

`Content-Type` 헤더는 브라우저가 multipart boundary를 자동 생성하도록 직접 설정하지 않는다.

### 3.2 성공 응답

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "transactions": [
    {
      "counterparty": "토스페이_TOSS",
      "originalAmount": 630,
      "personalAmount": 0,
      "transactionType": "ANOMALY",
      "purposeCategory": "OTHER",
      "anomaly": true,
      "anomalyReason": "AMBIGUOUS_PAYMENT_GATEWAY",
      "anomalyDetail": "결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다."
    }
  ],
  "summary": {
    "parsedCount": 1,
    "parsedAmount": 630,
    "expenseCount": 0,
    "expenseAmount": 0,
    "selfTransferAmount": 0,
    "otherPersonAmount": 0,
    "anomalyCount": 1,
    "anomalyAmount": 630
  },
  "categoryCatalogSource": "DATABASE",
  "dominantCategory": null
}
```

### 3.3 거래 필드

카테고리는 아래 `purposeCategory` 하나로 전달합니다. 별도 업종 필드는 반환하지 않습니다.
결제대행명은 카테고리가 아닌 이상치 조건이며, 기존 `anomalyReason`과 `anomalyDetail`을 사용합니다.

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `counterparty` | String | O | 가맹점, 이체 수신자 또는 결제 플랫폼 이름 |
| `originalAmount` | Long | O | OCR이 인식한 원본 거래 금액 |
| `personalAmount` | Long | O | 확정 소비 합계에 포함되는 본인 부담 금액 |
| `transactionType` | Enum String | O | 거래 분류 |
| `purposeCategory` | Enum String | O | 소비 목적 카테고리 |
| `anomaly` | Boolean | O | 이상치 감지 여부 |
| `anomalyReason` | Enum String | O | 이상치 판단 이유. 정상 거래는 `NONE` |
| `anomalyDetail` | String 또는 null | O | 사용자에게 표시할 이상치 설명. 정상 거래는 `null` |

### 3.4 요약 필드

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `parsedCount` | Integer | O | OCR로 파싱된 전체 거래 건수 |
| `parsedAmount` | Long | O | 파싱된 원본 금액 합계 |
| `expenseCount` | Integer | O | 확정 소비 건수 |
| `expenseAmount` | Long | O | 확정 소비 금액 합계 |
| `selfTransferAmount` | Long | O | 본인 계좌 이체로 분류된 금액 합계 |
| `otherPersonAmount` | Long | O | 다른 사람 거래로 분류된 금액 합계 |
| `anomalyCount` | Integer | O | 이상치 건수 |
| `anomalyAmount` | Long | O | 이상치 원본 금액 합계 |

### 3.5 카테고리 결과 필드

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `categoryCatalogSource` | Enum String | O | `DATABASE`: DB 기준 사용, `FALLBACK`: 내장 기본값 사용 |
| `dominantCategory` | Object 또는 null | O | 확정 소비가 없으면 `null` |
| `dominantCategory.purposeCategory` | Enum String | 조건부 | 최대 비중 소비 카테고리 |
| `dominantCategory.amount` | Long | 조건부 | 해당 카테고리의 확정 소비 합계 |
| `dominantCategory.ratioPercent` | Integer | 조건부 | 전체 확정 소비 중 비율을 반올림한 값 |
| `dominantCategory.gifUrl` | String | 조건부 | DB 또는 기본 카탈로그의 카테고리 GIF URL |

## 4. Enum 값

### 4.1 transactionType

| 값 | 의미 |
| --- | --- |
| `EXPENSE` | 확정 소비 |
| `SELF_TRANSFER` | 본인 계좌 이체 |
| `OTHER_PERSON` | 다른 사람의 거래로 판단된 항목 |
| `ANOMALY` | 자동 확정이 어려워 이상치로 표시한 항목 |

### 4.2 purposeCategory

| 값 | 의미 |
| --- | --- |
| `FOOD` | 식비 |
| `TRANSPORT` | 교통 |
| `LIVING` | 생활 |
| `SHOPPING` | 쇼핑 |
| `CULTURE_HOBBY` | 문화·취미 |
| `HEALTH` | 건강 |
| `EDUCATION` | 교육 |
| `FIXED_SUBSCRIPTION` | 고정비·구독 |
| `OTHER` | 기타 |

### 4.3 anomalyReason

| 값 | 의미 |
| --- | --- |
| `NONE` | 이상치가 아님 |
| `SELF_TRANSFER` | 거래 대상 이름이 계좌 소유자와 일치 |
| `AMBIGUOUS_PAYMENT_GATEWAY` | 결제 플랫폼명만으로 결제와 송금을 구분하기 어려움 |
| `GROUP_PAYMENT_CANDIDATE` | 단체 결제 가능성이 있는 큰 금액 |

## 5. 오류 응답

오류는 RFC 9457 Problem Details 형식으로 반환한다.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "At least one image is required",
  "instance": "/api/v1/analyses",
  "code": "INVALID_REQUEST"
}
```

| HTTP Status | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `INVALID_REQUEST` | 이름 누락·길이 오류, 이미지 누락·개수·형식·시그니처 오류 |
| `413 Content Too Large` | `IMAGE_TOO_LARGE` | 이미지 한 장 10MB 또는 요청 전체 50MB 초과 |
| `502 Bad Gateway` | `OCR_SERVICE_ERROR` | Spring이 Python OCR을 호출하지 못했거나 OCR 응답 검증 실패 |

DB 카테고리 조회 실패는 오류 응답을 만들지 않습니다. 내장 기본값으로 분석을 완료하고 `categoryCatalogSource: "FALLBACK"`을 반환합니다.

## 6. Frontend 처리 기준

- 성공 시 분석 응답을 메모리에 보관하고 `/result`로 이동한다.
- 결과 화면의 대표 GIF는 `dominantCategory.gifUrl`을 사용하고, URL 로드 실패 시 로컬 기본 GIF로 대체한다.
- 현재 서비스는 로그인, 쿠키, 사용자 ID, 분석 ID를 사용하지 않는다.
- 분석 결과 조회용 GET API와 수정용 PATCH API는 현재 제공하지 않는다.
- 프론트의 `ANALYSIS_TIMEOUT`은 Spring 오류 응답이 아니라 브라우저가 요청을 중단하면서 만드는 클라이언트 오류다.
- 서버의 `detail`은 사용자 메시지로 사용할 수 있고, 분기 처리는 `status`와 `code`를 기준으로 한다.

## 7. Swagger

Spring 실행 후 다음 주소에서 실제 multipart 요청을 테스트할 수 있다.

```text
http://localhost:8080/swagger.html
```
