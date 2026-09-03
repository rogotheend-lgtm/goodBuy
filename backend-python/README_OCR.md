# OCR 기반 소비 내역 파싱

금융 앱의 소비 내역 스크린샷을 **PaddleOCR → 거래내역 파싱 → JSON 변환**하는 Python 모듈입니다.

현재 POC의 목표는 특정 금융 앱의 UI를 고정적으로 파싱하는 것이 아니라, 다양한 금융 앱에서 공통적으로 나타나는 **거래 금액과 거래 상대방(counterparty)의 공간적 관계**를 이용해 소비 내역을 추출하는 것입니다.

## 1. 목표

입력:

```text
금융 앱 소비 내역 스크린샷
```

출력:

```json
{
  "transactions": [
    {
      "counterparty": "세븐일레븐 광주산정고려점",
      "amount": 16480,
      "accuracy": 0.99
    }
  ],
  "summary": {
    "total_count": 1,
    "total_amount": 16480
  }
}
```

> `accuracy`는 현재 POC에서 실제 구현하지 않았으며, 향후 AI fallback 확장을 고려해 도입 예정인 필드입니다. 의미상으로는 OCR 자체의 정확도가 아니라 **해당 거래 항목을 올바르게 추출했다고 판단하는 신뢰도(confidence)**를 의미하도록 설계합니다.

## 2. 전체 처리 구조

```text
                    main.py
                       │
                       ▼
                input image
                       │
                       ▼
                PaddleOCREngine
                       │
                       ▼
                  OCRToken[]
                       │
                       ▼
             TransactionParser
                 │           │
                 ▼           ▼
             amount.py   counterparty.py
                 │           │
                 └─────┬─────┘
                       ▼
                ExpenseItem[]
                       │
                       ▼
                  Summary
                       │
                       ▼
                     JSON
```

핵심 원칙:

1. PaddleOCR은 OCR만 담당
2. Parser는 OCR 결과를 거래내역으로 구조화
3. 음수 금액을 transaction anchor로 사용
4. 금액 주변의 OCR box 위치를 이용해 counterparty를 찾음
5. 시간, 날짜, 잔액, UI 텍스트 등은 거래 상대방 후보에서 제외
6. Summary는 OCR 결과를 믿지 않고 파싱된 거래내역에서 직접 계산
7. 향후 낮은 confidence의 거래만 AI fallback으로 전달

## 3. 프로젝트 구조

```text
backend-python/
│
├── requirements.txt
│
├── domain/
│   └── models.py
│
├── ocr/
│   ├── engine.py
│   └── token.py
│
├── parser/
│   ├── amount.py
│   ├── counterparty.py
│   └── transaction_parser.py
│
├── fallback/
│   └── ai_repairer.py
│
├── legacy/
│   └── # LEGACY FILES
│
├── main.py
└── app.py
```

### 파일별 역할

- **`main.py`**: 전체 프로그램 진입점. 이미지 → OCR → Parser → JSON 실행.
- **`domain/models.py`**: 최종 JSON 구조를 Pydantic 모델로 정의.
- **`ocr/token.py`**: OCR 텍스트, score, bounding box를 `OCRToken`으로 표현.
- **`ocr/engine.py`**: PaddleOCR 실행 및 결과를 `OCRToken[]`으로 변환.
- **`parser/amount.py`**: `-16,480원` 같은 소비 금액 추출.
- **`parser/counterparty.py`**: 금액 주변에서 거래 상대방 후보를 판단.
- **`parser/transaction_parser.py`**: 금액 anchor와 counterparty를 조합해 거래내역 생성.
- **`fallback/ai_repairer.py`**: 향후 낮은 confidence 거래를 AI로 보정하기 위한 interface. (AI - Ready)
- **`legacy/`**: 이전 구조의 코드 보관. 현재 POC에서는 사용하지 않음.

## 4. Counterparty 용어

프로젝트 전체에서 `merchant` 대신 **`counterparty`**를 canonical 용어로 사용합니다.

`merchant`는 카드 가맹점에 의미가 제한될 수 있지만, `counterparty`는 다음을 모두 포함할 수 있습니다.

```text
카드 가맹점
송금 수신자
계좌이체 상대방
결제 플랫폼
자동이체 대상
```

예:

```text
세븐일레븐 광주산정고려점
토스페이_TOSS
홍길동
KT
```

## 5. OCR Token과 Bounding Box

텍스트만 사용하는 것이 아니라 bounding box를 함께 사용합니다.

예:

```text
세븐일레븐 광주산정고려점
box=(207, 295, 720, 350)

-16,480원
box=(791, 295, 1015, 351)
```

두 텍스트의 Y 좌표가 거의 동일하므로 같은 transaction row일 가능성이 높습니다.

반면 시간이나 잔액은 다른 위치에 있으므로 후보에서 제외할 수 있습니다.

## 6. Transaction Anchor

현재 POC의 핵심 전략은 **음수 금액을 transaction anchor로 사용하는 것**입니다.

```text
-16,480원
-2,750원
-11,600원
-3,000원
...
```

각 금액을 찾은 뒤 주변 OCR을 검색합니다.

```text
┌───────────────────────────────────────┐
│ counterparty              amount       │
│ 세븐일레븐 광주산정고려점    -16,480원    │
└───────────────────────────────────────┘
```

특정 앱의 고정 좌표 대신 상대적인 위치 관계를 사용하기 때문에 여러 금융 앱으로 확장하기 쉽습니다.

## 7. 제외 대상

OCR에는 거래와 무관한 텍스트도 포함됩니다.

```text
KT 9:59
카드
관리
ELEVEN
21:14
1,331,242원
채우기
보내기
```

예를 들어:

- `21:14` → 시간
- `1,331,242원` → 잔액 가능성이 높은 양수 금액
- `채우기`, `보내기`, `관리` → UI 요소

단, 특정 문자열을 무조건 제외하는 방식은 장기적으로 최소화하고, **텍스트 형태 + OCR confidence + bounding box 위치**를 주요 판단 기준으로 사용합니다.

## 8. Summary 계산

Summary는 OCR이나 AI가 제공한 값을 그대로 사용하지 않습니다.

항상 최종 transactions를 기준으로 직접 계산합니다.

```python
total_count = len(transactions)

total_amount = sum(
    transaction.amount
    for transaction in transactions
)
```

따라서 거래 목록과 summary가 불일치하는 문제를 줄일 수 있습니다.

## 9. 현재 테스트 이미지

현재 테스트 이미지에서 OCR은 다음 10건의 소비 정보를 정상적으로 인식했습니다.

```text
세븐일레븐 광주산정고려점       -16,480원
세븐일레븐광주소촌이지점         -2,750원
버거앤타코                     -11,600원
벌크커피하남소촌점              -3,000원
맘스터치소촌점                 -7,900원
금호칼국수                     -7,500원
토스페이_TOSS                  -630원
토스페이_TOSS                  -620원
다이소                         -6,000원
389마트                        -1,200원
```

예상 summary:

```json
{
  "total_count": 10,
  "total_amount": 57680
}
```

## 10. Confidence

confidence 값은 AI fallback 확장을 위해 설계 요소로 추가했습니다.

```json
{
  "counterparty": "세븐일레븐 광주산정고려점",
  "amount": 16480,
  "accuracy": 0.99
}
```

confidence 계산 요소 Ex:

```text
OCR confidence
        +
amount pattern confidence
        +
counterparty confidence
        +
counterparty/amount Y 정렬
        +
counterparty/amount X 관계
        +
transaction row structure
        +
duplicate/logo penalty
        ↓
transaction confidence
```


## 11. AI Fallback

AI는 모든 요청에 사용하는 것이 아니라 **Parser가 불확실한 경우에만 사용하는 것**을 목표로 합니다.

```text
                이미지
                  │
                  ▼
              PaddleOCR
                  │
                  ▼
              Parser
                  │
          ┌───────┴───────┐
          │               │
      confidence ↑    confidence ↓
          │               │
          ▼               ▼
       그대로 반환     AI fallback
                          │
                          ▼
                       재검증
                          │
                          ▼
                    최종 transactions
```

Fallback 후보:

```text
- counterparty를 찾지 못함
- amount를 찾지 못함
- counterparty 후보가 여러 개임
- 후보 간 점수 차이가 작음
- OCR confidence가 낮음
- transaction row 구조가 불명확함
- 결과가 예상 구조와 불일치함
- schema validation 실패
```

AI의 역할은 전체 파싱을 무조건 맡기는 것이 아니라 **Parser가 해결하지 못한 ambiguity를 보정하는 것**입니다.

## 12. 확장성

목표는 특정 금융 앱에 종속되지 않는 generic parser입니다.

```text
카카오페이
토스
신한카드
삼성카드
국민카드
...
```

UI가 서로 달라도 공통적으로 존재하는:

```text
거래 상대방
+
소비 금액
+
공간적 관계
```

를 이용합니다.

앱별 UI 차이가 너무 큰 경우에만 별도의 adapter를 추가합니다.
(해당 컨셉은 팀 통합 테스트로 증명했습니다.)

## 13. '모듈' 실행 방법

가상환경을 활성화한 뒤:

```bash
python main.py
```

입력 이미지는 `main.py`에서 지정합니다. (프로젝트에서는 app.py로 FastAPI 실행)

```python
image_path = Path("Example file.png")
```


## 14. 개발 원칙

### UI 위치를 하드코딩하지 않는다

지양:

```python
if y == 295:
    ...
```

권장:

```text
amount와 counterparty의 Y 차이
amount와 counterparty의 X 관계
OCR box 크기
상대적 row 구조
```

### OCR과 Parser를 분리한다

```text
PaddleOCR
   ↓
OCRToken
   ↓
Parser
```

OCR 엔진이 변경되어도 Parser가 영향을 최소화하도록 합니다.

### AI를 Parser에 직접 결합하지 않는다

AI fallback은 별도의 interface/protocol로 분리합니다.

```text
TransactionParser
       │
       ▼
   AIRepairer
```

### Summary는 항상 재계산한다

```text
transactions
      ↓
summary
```

### 내부 진단 정보와 외부 API를 분리한다

개발 과정에서는 다음 정보를 내부적으로 유지할 수 있습니다.

```text
confidence
warnings
candidate list
selected candidate
fallback 여부
```

하지만 외부 API의 JSON Schema는 필요한 필드만 노출합니다.