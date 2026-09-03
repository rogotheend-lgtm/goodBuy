from __future__ import annotations

from dataclasses import dataclass

from domain.models import ExpenseItem, ExpenseParsedResult, Summary
from ocr.token import OCRToken

from .amount import parse_expense_amount
from .counterparty import is_bad_counterparty_candidate


@dataclass
class TransactionCandidate:
    amount: int
    amount_token: OCRToken
    counterparty: OCRToken


class TransactionParser:

    def __init__(
        self,
        image_width: float,
        image_height: float,
    ) -> None:

        self.image_width = image_width
        self.image_height = image_height

    def parse(
        self,
        tokens: list[OCRToken],
    ) -> ExpenseParsedResult:

        candidates: list[TransactionCandidate] = []

        # --------------------------------------------------
        # 1. 음수 금액을 거래 Anchor로 찾는다.
        # --------------------------------------------------

        amount_tokens: list[tuple[OCRToken, int]] = []

        for token in tokens:

            amount = parse_expense_amount(token.text)

            if amount is None:
                continue

            amount_tokens.append(
                (token, amount)
            )

        # --------------------------------------------------
        # 2. 각 금액 주변에서 counterparty를 찾는다.
        # --------------------------------------------------

        for amount_token, amount in amount_tokens:

            counterparty = self._find_counterparty(
                amount_token=amount_token,
                tokens=tokens,
            )

            if counterparty is None:
                continue

            candidates.append(
                TransactionCandidate(
                    amount=amount,
                    amount_token=amount_token,
                    counterparty=counterparty,
                )
            )

        # --------------------------------------------------
        # 3. 최종 ExpenseItem 생성
        # --------------------------------------------------

        transactions: list[ExpenseItem] = []

        for candidate in candidates:

            transactions.append(
                ExpenseItem(
                    counterparty=candidate.counterparty.text,
                    amount=candidate.amount,
                )
            )

        # --------------------------------------------------
        # 4. Summary는 OCR/AI 값을 믿지 않고 직접 계산
        # --------------------------------------------------

        total_count = len(transactions)

        total_amount = sum(
            item.amount
            for item in transactions
        )

        return ExpenseParsedResult(
            transactions=transactions,
            summary=Summary(
                total_count=total_count,
                total_amount=total_amount,
            ),
        )

    # ======================================================
    # counterparty 탐색
    # ======================================================

    def _find_counterparty(
        self,
        amount_token: OCRToken,
        tokens: list[OCRToken],
    ) -> OCRToken | None:

        candidates: list[tuple[float, OCRToken]] = []

        for token in tokens:

            # 자기 자신은 제외
            if token is amount_token:
                continue

            # 빈 문자열 제외
            if not token.text.strip():
                continue

            # counterparty가 아니라고 판단되는 텍스트 제외
            if is_bad_counterparty_candidate(token):
                continue

            # --------------------------------------------------
            # 금액보다 오른쪽에 있는 텍스트는 제외
            # --------------------------------------------------

            if token.x1 >= amount_token.x1:
                continue

            # --------------------------------------------------
            # Y 중심점 차이
            # --------------------------------------------------

            y_distance = abs(
                token.cy - amount_token.cy
            )

            # 현재 화면의 거래 row 높이를 고려한 기준
            max_y_distance = max(
                amount_token.height * 1.5,
                40.0,
            )

            if y_distance > max_y_distance:
                continue

            # --------------------------------------------------
            # X 거리
            #
            # counterparty가 amount에 가까울수록 유리
            # --------------------------------------------------

            x_distance = (
                amount_token.x1 - token.x2
            )

            if x_distance < 0:
                x_distance = 0

            # --------------------------------------------------
            # 점수 계산
            # --------------------------------------------------

            y_score = max(
                0.0,
                1.0 - (
                    y_distance / max_y_distance
                ),
            )

            x_score = max(
                0.0,
                1.0 - (
                    x_distance / self.image_width
                ),
            )

            confidence_score = token.score

            score = (
                y_score * 0.55
                + x_score * 0.20
                + confidence_score * 0.25
            )

            candidates.append(
                (score, token)
            )

        # 후보가 없으면 실패
        if not candidates:
            return None

        # 점수가 가장 높은 counterparty
        candidates.sort(
            key=lambda item: item[0],
            reverse=True,
        )

        return candidates[0][1]