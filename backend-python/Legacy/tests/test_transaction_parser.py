from ocr.token import OCRToken
from parser.transaction_parser import TransactionParser


def token(text: str, x1: float, y1: float, x2: float, y2: float) -> OCRToken:
    return OCRToken(text, 0.99, x1, y1, x2, y2)


def test_parses_mobile_bank_layout_with_counterparty_above_amount() -> None:
    tokens = [
        token("이명로 → 메가엠지씨커피광주", 123, 155, 406, 183),
        token("*6", 39, 175, 92, 216),
        token("-8,300원", 123, 197, 248, 234),
        token("송금 | 토스머니", 120, 296, 265, 327),
        token("-30,000원", 122, 340, 266, 377),
    ]

    result = TransactionParser(image_width=720, image_height=1280).parse(tokens)

    assert result.model_dump() == {
        "transactions": [
            {"counterparty": "메가엠지씨커피광주", "amount": 8300},
            {"counterparty": "송금 | 토스머니", "amount": 30000},
        ],
        "summary": {"total_count": 2, "total_amount": 38300},
    }


def test_keeps_existing_same_row_layout() -> None:
    tokens = [
        token("벌크커피", 20, 20, 130, 45),
        token("-3,000원", 180, 20, 270, 45),
    ]

    result = TransactionParser(image_width=300, image_height=100).parse(tokens)

    assert result.transactions[0].counterparty == "벌크커피"
    assert result.transactions[0].amount == 3000
