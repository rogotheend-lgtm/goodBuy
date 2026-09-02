import tempfile
from pathlib import Path
from contextlib import asynccontextmanager
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image

from ocr.engine import PaddleOCREngine
from parser.transaction_parser import TransactionParser
from domain.models import ExpenseParsedResult  

ocr_engine: PaddleOCREngine = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global ocr_engine
    print("Initializing PaddleOCR Engine...")
    ocr_engine = PaddleOCREngine()
    print("PaddleOCR Engine initialization complete.")
    yield
    print("Shutting down OCR Service...")


app = FastAPI(title="OCR Expense Parser Service", lifespan=lifespan)


@app.post("/ocr/extraction", response_model=ExpenseParsedResult)
async def extract_transactions(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image.")

    # 1. 임시 파일(Temp File) 생성 후 이미지 저장
    # delete=True로 설정 시 with 블록이 끝날 때 디스크에서 자동 삭제됩니다.
    with tempfile.NamedTemporaryFile(delete=True, suffix=".png") as tmp_file:
        contents = await file.read()
        tmp_file.write(contents)
        tmp_file.flush()  # 버퍼의 내용을 디스크에 즉시 작성

        temp_image_path = Path(tmp_file.name)

        try:
            # 2. 이미지 크기(Width, Height) 측정
            with Image.open(temp_image_path) as img:
                width, height = img.size

            # 3. Path 객체 전달 (main.py 방식과 동일하게 동작)
            tokens = ocr_engine.recognize(temp_image_path)

            # 4. Transaction Parser 실행
            parser = TransactionParser(
                image_width=width,
                image_height=height,
            )
            result = parser.parse(tokens)

            return result

        except Exception as e:
            raise HTTPException(status_code=500, detail=f"OCR Parsing failed: {str(e)}")


@app.get("/health")
async def health_check():
    return {"status": "ok", "engine_loaded": ocr_engine is not None}