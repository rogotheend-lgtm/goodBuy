from __future__ import annotations

import json
import re
from dataclasses import dataclass

from paddleocr import PaddleOCR

from schema import ExpenseItem, ExpenseParsedResult, Summary


@dataclass
class OCRToken:
    text: str
    score: float
    x1: float
    y1: float
    x2: float
    y2: float

    @property
    def cx(self) -> float:
        return (self.x1 + self.x2) / 2

    @property
    def cy(self) -> float:
        return (self.y1 + self.y2) / 2


@dataclass
class TransactionCandidate:
    counterparty: str
    amount: int
    y: float
    confidence: float


AMOUNT_PATTERN = re.compile(
    r"^[−-]\s*([\d,]+)\s*원?$"
)

TIME_PATTERN = re.compile(
    r"^\d{1,2}:\d{2}$"
)

DATE_PATTERN = re.compile(
    r"^\d{1,2}월\s*\d{1,2}일$"
)


def create_ocr() -> PaddleOCR:

    return PaddleOCR(
        lang="korean",
        ocr_version="PP-OCRv5",

        use_doc_orientation_classify=False,
        use_doc_unwarping=False,
        use_textline_orientation=False,
    )


def extract_tokens(
    ocr: PaddleOCR,
    image_path: str,
) -> list[OCRToken]:

    results = ocr.predict(image_path)

    tokens: list[OCRToken] = []

    for result in results:

        data = result.json

        if isinstance(data, str):
            data = json.loads(data)

        res = data["res"]

        texts = res.get("rec_texts", [])
        scores = res.get("rec_scores", [])
        boxes = res.get("rec_boxes", [])

        for text, score, box in zip(
            texts,
            scores,
            boxes,
        ):

            text = str(text).strip()

            if not text:
                continue

            x1, y1, x2, y2 = map(float, box)

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


def parse_amount(text: str) -> int | None:

    text = text.strip()

    match = AMOUNT_PATTERN.match(text)

    if not match:
        return None

    return int(
        match.group(1).replace(",", "")
    )


def is_noise(text: str) -> bool:

    text = text.strip()

    if TIME_PATTERN.match(text):
        return True

    if DATE_PATTERN.match(text):
        return True

    if re.match(r"^[\d,]+원$", text):
        return True

    return False


def find_counterparty(
    amount_token: OCRToken,
    tokens: list[OCRToken],
) -> OCRToken | None:

    candidates = []

    for token in tokens:

        if token is amount_token:
            continue

        if is_noise(token.text):
            continue

        # 금액보다 오른쪽이면 상호명이 아님
        if token.cx >= amount_token.cx:
            continue

        # 아이콘 영역
        if token.cx < 120:
            continue

        # 금액과 같은 행인지 검사
        y_distance = abs(
            token.cy - amount_token.cy
        )

        if y_distance > 50:
            continue

        candidates.append(token)

    if not candidates:
        return None

    # 가장 가까운 Y + 높은 confidence
    candidates.sort(
        key=lambda token: (
            abs(token.cy - amount_token.cy),
            -token.score,
        )
    )

    return candidates[0]


def parse_expenses(
    tokens: list[OCRToken],
) -> ExpenseParsedResult:

    candidates: list[TransactionCandidate] = []

    for amount_token in tokens:

        amount = parse_amount(
            amount_token.text
        )

        if amount is None:
            continue

        # 화면 오른쪽 금액 영역
        if amount_token.cx < 0.60 * 945:
            continue

        counterparty_token = find_counterparty(
            amount_token,
            tokens,
        )

        if counterparty_token is None:
            continue

        candidates.append(
            TransactionCandidate(
                counterparty=counterparty_token.text,
                amount=amount,
                y=amount_token.cy,
                confidence=min(
                    amount_token.score,
                    counterparty_token.score,
                ),
            )
        )

    # 화면 위 → 아래
    candidates.sort(
        key=lambda x: x.y
    )

    transactions = [
        ExpenseItem(
            counterparty=c.counterparty,
            amount=c.amount,
        )
        for c in candidates
    ]

    return ExpenseParsedResult(
        transactions=transactions,
        summary=Summary(
            total_count=len(transactions),
            total_amount=sum(
                item.amount
                for item in transactions
            ),
        ),
    )