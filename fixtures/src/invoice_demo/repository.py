from .models import Invoice


class InvoiceRepository:
    def __init__(self) -> None:
        self._invoices: dict[str, Invoice] = {}

    def save(self, invoice: Invoice) -> None:
        self._invoices[invoice.invoice_id] = invoice

    def get(self, invoice_id: str) -> Invoice | None:
        return self._invoices.get(invoice_id)

    def list_for_customer(self, customer_id: str) -> list[Invoice]:
        return [
            invoice
            for invoice in self._invoices.values()
            if invoice.customer_id == customer_id
        ]
