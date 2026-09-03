from __future__ import annotations

from typing import Protocol

from domain.models import ExpenseParsedResult
from ocr.token import OCRToken


class AIRepairer(Protocol):

    def repair(
        self,
        tokens: list[OCRToken],
        partial_result: ExpenseParsedResult,
    ) -> ExpenseParsedResult:
        ...