from decimal import Decimal
from uuid import uuid4

from .models import Invoice, InvoiceLine
from .repository import InvoiceRepository


class InvoiceService:
    def __init__(self, repository: InvoiceRepository) -> None:
        self._repository = repository

    def create_invoice(
        self,
        customer_id: str,
        line_items: list[tuple[str, int, str]],
    ) -> Invoice:
        lines = tuple(
            InvoiceLine(
                description=description,
                quantity=quantity,
                unit_price=Decimal(unit_price),
            )
            for description, quantity, unit_price in line_items
        )
        self._validate(customer_id, lines)
        invoice = Invoice(
            invoice_id=str(uuid4()),
            customer_id=customer_id,
            lines=lines,
        )
        self._repository.save(invoice)
        return invoice

    def _validate(self, customer_id: str, lines: tuple[InvoiceLine, ...]) -> None:
        if not customer_id.strip():
            raise ValueError("customer_id is required")
        if len(lines) == 0:
            raise ValueError("at least one invoice line is required")
        for line in lines:
            if line.quantity <= 0:
                raise ValueError("line quantity must be positive")
            if line.unit_price <= Decimal("0"):
                raise ValueError("line unit price must be positive")
