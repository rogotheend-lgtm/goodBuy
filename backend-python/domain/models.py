from pydantic import BaseModel, ConfigDict, Field


class ExpenseItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    counterparty: str = Field(
        ...,
        max_length=100,
    )

    amount: int = Field(
        ...,
        ge=0,
    )


class Summary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    total_count: int = Field(
        ...,
        ge=0,
    )

    total_amount: int = Field(
        ...,
        ge=0,
    )


class ExpenseParsedResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    transactions: list[ExpenseItem]

    summary: Summary