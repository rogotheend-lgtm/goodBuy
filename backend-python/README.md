# goodBuy Python OCR Backend

Spring에서 받은 거래내역 이미지 한 장을 OCR로 읽고 거래 대상·금액 JSON을 반환하는 내부 FastAPI 서비스입니다.

## 통합 과정에서 수정한 사항

`lim` 브랜치의 Spring·Frontend 통합 테스트 중 실제 모바일 은행 캡처가 약 40초 뒤 거래 0건으로 반환되는 문제가 확인되어 Python 영역을 함께 수정했습니다.

- CPU에서 느린 기본 server 검출 모델 대신 `PP-OCRv5_mobile_det`을 사용합니다.
- OCR 입력의 긴 변을 최대 1280px로 제한해 화면 글자를 유지하면서 연산량을 줄였습니다.
- 상호명과 금액이 같은 줄인 기존 화면뿐 아니라, 상호명이 위이고 금액이 아래인 모바일 은행 화면도 파싱합니다.
- `보낸 사람 → 거래 대상` 형식은 오른쪽의 실제 거래 대상만 반환하고, 로고 오인식은 거래 대상 후보에서 제외합니다.

Spring과 합의된 `POST /ocr/extraction` 요청·응답 JSON 계약은 변경하지 않았습니다. 실제 문제 이미지 기준으로 OCR 구간은 약 40.5초에서 2.2초로 단축되었고, 거래 0건 대신 음수 거래 4건을 반환하는 것을 확인했습니다.

## API 계약

```http
POST /ocr/extraction
Content-Type: multipart/form-data

file: PNG 또는 JPEG 이미지, 최대 10MB
```

응답:

```json
{
  "transactions": [
    {"counterparty": "벌크커피", "amount": 3000}
  ],
  "summary": {
    "total_count": 1,
    "total_amount": 3000
  }
}
```

헬스체크는 `GET /health`, Swagger UI는 `http://localhost:8000/docs`입니다.

## 통신 검증용 mock 실행

PaddleOCR 설치 없이 Spring-Python HTTP 연결을 먼저 확인할 때 사용합니다.

```bash
cd /Users/lim/goodBuy/backend-python
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements-api.txt
OCR_MODE=mock .venv/bin/python -m uvicorn app:app --host 0.0.0.0 --port 8000
```

## 실제 PaddleOCR 실행

```bash
cd /Users/lim/goodBuy/backend-python
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
OCR_MODE=paddle .venv/bin/python -m uvicorn app:app --host 0.0.0.0 --port 8000
```

Spring은 다음 설정으로 Python을 호출합니다.

```bash
OCR_MODE=python OCR_BASE_URL=http://localhost:8000 ./gradlew bootRun
```

## 테스트

```bash
.venv/bin/python -m pytest -q
```
