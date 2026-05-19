from decimal import Decimal

import pytest

from invoice_demo.repository import InvoiceRepository
from invoice_demo.service import InvoiceService


def test_create_invoice_persists_and_returns_invoice() -> None:
    repository = InvoiceRepository()
    service = InvoiceService(repository)

    invoice = service.create_invoice(
        customer_id="customer-001",
        line_items=[("Design work", 2, "125.00")],
    )

    assert invoice.total == Decimal("250.00")
    assert repository.get(invoice.invoice_id) == invoice


def test_create_invoice_rejects_empty_line_items() -> None:
    repository = InvoiceRepository()
    service = InvoiceService(repository)

    with pytest.raises(ValueError, match="at least one invoice line"):
        service.create_invoice("customer-001", [])
