import re

from ocr.token import OCRToken

from .amount import parse_expense_amount


TIME_PATTERN = re.compile(
    r"^\d{1,2}:\d{2}$"
)

DATE_PATTERN = re.compile(
    r"^\d{1,2}월\s*\d{1,2}일$"
)

POSITIVE_AMOUNT_PATTERN = re.compile(
    r"^[\d,.]+\s*원$"
)

ARROW_PATTERN = re.compile(r"\s*(?:→|->|⇒)\s*")
MEANINGFUL_CHARACTER_PATTERN = re.compile(r"[가-힣A-Za-z0-9]")


def is_bad_counterparty_candidate(
    token: OCRToken,
) -> bool:

    text = token.text.strip()

    if not text:
        return True

    if TIME_PATTERN.match(text):
        return True

    if DATE_PATTERN.match(text):
        return True

    if POSITIVE_AMOUNT_PATTERN.match(text):
        return True

    if parse_expense_amount(text) is not None:
        return True

    # 앱 로고가 '*6', '*b'처럼 OCR되는 경우를 거래 대상에서 제외합니다.
    if len(MEANINGFUL_CHARACTER_PATTERN.findall(text)) < 2:
        return True

    return False


def normalize_counterparty(text: str) -> str:
    """'보낸 사람 → 거래 대상' 형식에서는 실제 거래 대상만 반환합니다."""
    parts = ARROW_PATTERN.split(text.strip())
    if len(parts) > 1 and parts[-1].strip():
        return parts[-1].strip()
    return text.strip()
