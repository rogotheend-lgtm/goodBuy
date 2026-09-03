import os
import tempfile
from asyncio import Lock
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Callable, Protocol

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from PIL import Image, UnidentifiedImageError
from starlette.concurrency import run_in_threadpool

from domain.models import ExpenseParsedResult
from ocr.token import OCRToken
from parser.transaction_parser import TransactionParser


MAX_IMAGE_BYTES = 10 * 1024 * 1024
# 모바일 거래내역은 글자가 충분히 크므로 긴 변을 1280px로 제한해도
# 금액 인식률을 유지하면서 CPU OCR 시간을 크게 줄일 수 있습니다.
MAX_OCR_IMAGE_DIMENSION = 1280
SUPPORTED_IMAGE_TYPES = {"image/png": ".png", "image/jpeg": ".jpg"}


class OcrEngine(Protocol):
    def recognize(self, image_path: str | Path) -> list[OCRToken]: ...


class MockOcrEngine:
    """Paddle 설치 없이 Spring-Python HTTP 연결을 검증하는 엔진입니다."""

    def recognize(self, image_path: str | Path) -> list[OCRToken]:
        return [
            OCRToken("벌크커피", 0.99, 20, 20, 130, 45),
            OCRToken("-3,000원", 0.99, 180, 20, 270, 45),
            OCRToken("토스페이_TOSS", 0.99, 20, 70, 150, 95),
            OCRToken("-630원", 0.99, 190, 70, 270, 95),
        ]


def prepare_image_for_ocr(image_path: Path, suffix: str) -> tuple[int, int]:
    """큰 이미지만 축소해 OCR 계산량을 제한하고, 실제 분석 크기를 반환합니다."""
    with Image.open(image_path) as image:
        image.verify()

    resized_image: Image.Image | None = None
    with Image.open(image_path) as image:
        image.load()
        if max(image.size) > MAX_OCR_IMAGE_DIMENSION:
            image.thumbnail(
                (MAX_OCR_IMAGE_DIMENSION, MAX_OCR_IMAGE_DIMENSION),
                Image.Resampling.LANCZOS,
            )
            resized_image = image.copy()
        width, height = image.size

    if resized_image is not None:
        if suffix == ".jpg" and resized_image.mode not in {"RGB", "L"}:
            resized_image = resized_image.convert("RGB")
        if suffix == ".jpg":
            resized_image.save(image_path, quality=90)
        else:
            resized_image.save(image_path)

    return width, height


def build_engine() -> OcrEngine:
    mode = os.getenv("OCR_MODE", "paddle").lower()
    if mode == "mock":
        return MockOcrEngine()
    if mode != "paddle":
        raise RuntimeError("OCR_MODE must be 'paddle' or 'mock'")

    # mock 통신 테스트에서는 무거운 Paddle 의존성을 불러오지 않습니다.
    from ocr.engine import PaddleOCREngine

    return PaddleOCREngine()


def create_app(engine_factory: Callable[[], OcrEngine] = build_engine) -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI):
        application.state.ocr_engine = engine_factory()
        # PaddleOCR 엔진은 한 번에 한 요청만 실행합니다. 연산은 별도 스레드에서
        # 수행해 다음 HTTP 응답이나 health 요청이 이벤트 루프를 막지 않게 합니다.
        application.state.ocr_lock = Lock()
        yield

    application = FastAPI(
        title="goodBuy OCR Service",
        description="거래내역 이미지에서 거래 대상과 금액을 추출하는 내부 서비스",
        version="v1",
        lifespan=lifespan,
    )

    @application.post("/ocr/extraction", response_model=ExpenseParsedResult)
    async def extract_transactions(
        request: Request,
        file: UploadFile = File(..., description="PNG 또는 JPEG 거래내역 이미지"),
    ) -> ExpenseParsedResult:
        suffix = SUPPORTED_IMAGE_TYPES.get(file.content_type or "")
        if suffix is None:
            raise HTTPException(status_code=400, detail="Only PNG and JPEG images are supported")

        contents = await file.read(MAX_IMAGE_BYTES + 1)
        if not contents:
            raise HTTPException(status_code=400, detail="Image file is empty")
        if len(contents) > MAX_IMAGE_BYTES:
            raise HTTPException(status_code=413, detail="Image file must not exceed 10MB")

        temp_path: Path | None = None
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
                temp_file.write(contents)
                temp_path = Path(temp_file.name)

            width, height = prepare_image_for_ocr(temp_path, suffix)

            async with request.app.state.ocr_lock:
                tokens = await run_in_threadpool(
                    request.app.state.ocr_engine.recognize,
                    temp_path,
                )
            return TransactionParser(image_width=width, image_height=height).parse(tokens)
        except UnidentifiedImageError as exception:
            raise HTTPException(status_code=400, detail="File content is not a valid image") from exception
        except HTTPException:
            raise
        except Exception as exception:
            raise HTTPException(status_code=500, detail="OCR parsing failed") from exception
        finally:
            if temp_path is not None:
                temp_path.unlink(missing_ok=True)

    @application.get("/health")
    async def health_check(request: Request) -> dict[str, object]:
        return {
            "status": "ok",
            "mode": os.getenv("OCR_MODE", "paddle").lower(),
            "engineLoaded": hasattr(request.app.state, "ocr_engine"),
        }

    return application


app = create_app()
