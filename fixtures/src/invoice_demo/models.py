from dataclasses import dataclass
from decimal import Decimal


@dataclass(frozen=True)
class InvoiceLine:
    description: str
    quantity: int
    unit_price: Decimal

    @property
    def total(self) -> Decimal:
        return self.unit_price * self.quantity


@dataclass(frozen=True)
class Invoice:
    invoice_id: str
    customer_id: str
    lines: tuple[InvoiceLine, ...]

    @property
    def total(self) -> Decimal:
        return sum((line.total for line in self.lines), Decimal("0.00"))
