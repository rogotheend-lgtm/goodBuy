# goodBuy Python OCR Backend

Spring에서 받은 거래내역 이미지 한 장을 OCR로 읽고 거래 대상·금액 JSON을 반환하는 내부 FastAPI 서비스입니다.

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
