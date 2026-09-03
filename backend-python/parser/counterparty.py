import re

from ocr.token import OCRToken


TIME_PATTERN = re.compile(
    r"^\d{1,2}:\d{2}$"
)

DATE_PATTERN = re.compile(
    r"^\d{1,2}월\s*\d{1,2}일$"
)

POSITIVE_AMOUNT_PATTERN = re.compile(
    r"^[\d,.]+\s*원$"
)


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

    return False