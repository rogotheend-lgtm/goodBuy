from __future__ import annotations

import json
from pathlib import Path

from paddleocr import PaddleOCR

from .token import OCRToken


class PaddleOCREngine:

    def __init__(self) -> None:
        self._ocr = PaddleOCR(
            # CPU 환경에서 기본 server 검출 모델은 한 장에 수십 초가 걸립니다.
            # 모바일 검출 모델과 한국어 인식 모델 조합은 화면 캡처의 큰 글자를
            # 훨씬 빠르게 읽으면서 필요한 거래명과 금액 인식률을 유지합니다.
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_recognition_model_name="korean_PP-OCRv5_mobile_rec",

            # 스크린샷/일반 이미지에서는
            # 문서 방향/warping은 일단 사용하지 않는다.
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
        )

    def recognize(
        self,
        image_path: str | Path,
    ) -> list[OCRToken]:

        image_path = Path(image_path)

        if not image_path.exists():
            raise FileNotFoundError(image_path)

        results = self._ocr.predict(
            str(image_path)
        )

        tokens: list[OCRToken] = []

        for result in results:

            data = result.json

            if isinstance(data, str):
                data = json.loads(data)

            res = data["res"]

            texts = res.get(
                "rec_texts",
                [],
            )

            scores = res.get(
                "rec_scores",
                [],
            )

            boxes = res.get(
                "rec_boxes",
                [],
            )

            for text, score, box in zip(
                texts,
                scores,
                boxes,
            ):

                text = str(text).strip()

                if not text:
                    continue

                x1, y1, x2, y2 = map(
                    float,
                    box,
                )

                tokens.append(
                    OCRToken(
                        text=text,
                        score=float(score),
                        x1=x1,
                        y1=y1,
                        x2=x2,
                        y2=y2,
                    )
                )

        return tokens
