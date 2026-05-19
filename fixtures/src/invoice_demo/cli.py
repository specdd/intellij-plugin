from .repository import InvoiceRepository
from .service import InvoiceService


def create_demo_invoice() -> str:
    repository = InvoiceRepository()
    service = InvoiceService(repository)
    invoice = service.create_invoice(
        customer_id="customer-001",
        line_items=[("Design work", 2, "125.00")],
    )
    return f"{invoice.invoice_id}: {invoice.total}"


if __name__ == "__main__":
    print(create_demo_invoice())
