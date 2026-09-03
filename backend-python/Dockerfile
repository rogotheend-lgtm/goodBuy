# 파이썬 3.11 지원 명세에 맞춘 베이스 이미지
FROM python:3.11-slim

# PaddleOCR 및 OpenCV 동작에 필수적인 OS 시스템 라이브러리 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 의존성 패키지 복사 및 설치
COPY requirements.txt .
RUN ls -la
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 전체 복사
COPY . .

EXPOSE 8000

# FastAPI / Uvicorn 실행
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]