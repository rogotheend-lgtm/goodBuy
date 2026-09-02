from pydantic import BaseModel, Field, ConfigDict


class ExpenseItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    counterparty: str = Field(
        ...,
        max_length=100,
        description="거래 대상"
    )

    amount: int = Field(
        ...,
        ge=0,
        description="소비 금액"
    )


class Summary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    total_count: int = Field(
        ...,
        ge=0
    )

    total_amount: int = Field(
        ...,
        ge=0
    )


class ExpenseParsedResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    transactions: list[ExpenseItem]
    summary: Summary