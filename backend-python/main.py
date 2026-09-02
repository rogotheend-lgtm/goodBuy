from pathlib import Path

from PIL import Image

from ocr.engine import PaddleOCREngine
from parser.transaction_parser import (
    TransactionParser,
)


IMAGE_PATH = Path("userEX.png")


def main():

    # -------------------------------------------------
    # 1. OCR
    # -------------------------------------------------

    ocr = PaddleOCREngine()

    tokens = ocr.recognize(
        IMAGE_PATH
    )

    print(
        f"OCR tokens: {len(tokens)}"
    )

    for token in tokens:

        print(
            f"{token.text:30} "
            f"score={token.score:.3f} "
            f"box=({token.x1:.0f}, "
            f"{token.y1:.0f}, "
            f"{token.x2:.0f}, "
            f"{token.y2:.0f})"
        )

    # -------------------------------------------------
    # 2. Image dimensions
    # -------------------------------------------------

    with Image.open(IMAGE_PATH) as image:

        width, height = image.size

    # -------------------------------------------------
    # 3. Transaction parsing
    # -------------------------------------------------

    parser = TransactionParser(
        image_width=width,
        image_height=height,
    )

    result = parser.parse(tokens)

    # -------------------------------------------------
    # 4. Output
    # -------------------------------------------------

    print()
    print(
        result.model_dump_json(
            indent=2,
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()