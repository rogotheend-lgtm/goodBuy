from dataclasses import dataclass


@dataclass(frozen=True)
class ParseConfidence:

    merchant: float
    amount: float
    structure: float

    @property
    def overall(self) -> float:

        return (
            self.merchant * 0.45
            + self.amount * 0.35
            + self.structure * 0.20
        )

    @property
    def requires_fallback(self) -> bool:

        return self.overall < 0.80