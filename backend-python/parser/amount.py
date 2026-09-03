import re


# 일반적인 한국 원화 지출 표시
#
# -16,480원
# -2,750원
# −16,480원
# -16480원
#
AMOUNT_PATTERN = re.compile(
    r"^[\s]*[-−]\s*([\d,]+)\s*원?\s*$"
)


def parse_expense_amount(
    text: str,
) -> int | None:

    match = AMOUNT_PATTERN.match(
        text.strip()
    )

    if not match:
        return None

    value = match.group(1)

    value = value.replace(",", "")

    try:
        amount = int(value)
    except ValueError:
        return None

    if amount <= 0:
        return None

    return amount