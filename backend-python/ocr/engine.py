from __future__ import annotations

import json
from pathlib import Path

from paddleocr import PaddleOCR

from .token import OCRToken


class PaddleOCREngine:

    def __init__(self) -> None:
        self._ocr = PaddleOCR(
            lang="korean",

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