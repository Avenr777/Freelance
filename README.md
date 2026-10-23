# Freelancer Ledger & Invoice Generator

A Java 21 / JavaFX desktop application for managing freelance clients, projects, work logs, invoices, and payments. It uses JDBC with PostgreSQL support, and defaults to an embedded H2 database so it can be run immediately.

## Run the application

Requirements: JDK 21 and Maven (a local Maven distribution is included in `tools/maven`).

```powershell
.\tools\maven\apache-maven-3.9.6\bin\mvn.cmd clean javafx:run
```

To run the tests:

```powershell
.\tools\maven\apache-maven-3.9.6\bin\mvn.cmd test
```

## PostgreSQL setup

Create a PostgreSQL database, then update `db.properties` (or supply equivalent JVM `-D` properties):

```properties
db.type=postgres
db.postgres.url=jdbc:postgresql://localhost:5432/freelance_db
db.postgres.user=your_user
db.postgres.password=your_password
```

At startup, `DatabaseInitializer` reads `src/main/resources/sql/schema.sql` and creates or upgrades the tables. The JDBC driver, `DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`, generated keys, joins, aggregate queries, and try-with-resources are used throughout the DAO layer. H2 remains available for local use and automated tests.

## Main features

- Client, project and work-log management
- Automatic invoice creation from logged hours × hourly rate
- PDF invoice export
- Invoice status tracking: Draft, Sent, Paid, and derived Overdue
- Partial payments: select an invoice, choose **Record Payment**, and enter an amount up to the outstanding balance. Every payment is kept in the `payments` table; an invoice becomes `PAID` only when the balance reaches zero.
- Payment reminders: **Create Reminders** produces a timestamped, ready-to-send queue for sent/overdue invoices without freezing the UI. Each entry includes the client contact, invoice/project, amount paid, balance due, due date, and an overdue/due-soon label.
- Multiple tax rates, percentage or fixed-amount discounts, and custom invoice notes are supported by the invoice model/database. `Invoice.calculateFinalAmount()` applies discount first and tax second.
- Project budgets: each project has an optional budget amount, stored alongside its hourly rate.
- Client-wise revenue report: `RevenueReportDao.findClientRevenue()` returns each client’s invoiced, received, and outstanding amounts using an aggregate JDBC query.

## Course-topic mapping

| Topic | How it is demonstrated |
| --- | --- |
| OOP | Domain classes (`Client`, `Project`, `WorkLog`, `Invoice`, `Payment`), enums (`InvoiceStatus`, `DiscountType`), encapsulation through private fields/getters/setters, immutable record DTOs (`Payment`, `ClientRevenue`), and separate DAO/service/controller responsibilities. |
| Exception handling | Validation rejects invalid money values and overpayments; DAO methods propagate `SQLException`; UI handlers catch failures and show understandable alerts; database/schema and file failures are wrapped with meaningful messages. |
| File handling | The schema is loaded as a UTF-8 resource, PDFs are exported with OpenPDF, and the reminder service writes a UTF-8 text queue with `Files.writeString`. |
| Multithreading | `PaymentReminderService.createReminderFileAsync()` creates and starts a named `Thread` (`payment-reminder-generator`) so file generation does not block the UI; its `CompletableFuture` completion alert is returned to JavaFX’s UI thread through `Platform.runLater`. |
| JDBC + PostgreSQL | PostgreSQL JDBC driver dependency, configurable PostgreSQL connection, prepared statements, generated IDs, foreign keys, joins, and report aggregates. |

## Database design

- `clients` → `projects` → `work_logs` / `invoices`
- `payments` belongs to an `invoice` and supports many payments per invoice.
- `projects.budget_amount` supports budget tracking.
- `invoices.tax_rate`, `discount_type`, `discount_value`, and `notes` support flexible invoice pricing and payment notes.

The schema contains idempotent `CREATE TABLE IF NOT EXISTS` statements and additive migration statements, allowing an older local database to be upgraded on the next startup.
