# Payroll & Tax Calculator with Persistence & Analytics

A professional, portfolio-grade HRMS payroll management system built in
**Java 17**, **Swing + FlatLaf**, and **MySQL/JDBC**, following a clean
**MVC + DAO + Service-layer** architecture.

![Java](https://img.shields.io/badge/Java-17-orange) ![Maven](https://img.shields.io/badge/Build-Maven-blue) ![MySQL](https://img.shields.io/badge/DB-MySQL-4479A1)

---

## 1. What's implemented (fully working)

- **Authentication** — login screen, show/hide password, remember-me, validation, logout, BCrypt-hashed passwords.
- **Dashboard** — 6 live KPI cards (employees, monthly payroll, tax collected, avg/max/min salary) + recent activity feed.
- **Employee Management** — add / edit / (soft) delete / search / filter by department / filter by salary range / view details, with full field validation and a salary-change audit trail (`salary_history` table).
- **Payroll Calculator** — Gross = Basic + Bonus + Allowances; PF deduction; progressive slab-based income tax; Net salary. Supports single-employee calculation and a bulk "Run Payroll for All" action, persisted to the `payroll` table.
- **Tax Calculator** — fully editable progressive tax slabs (add/edit/remove brackets), applied live to every subsequent payroll run.
- **Salary Slip** — professional printable PDF payslip (OpenPDF) with company header, employee details, and full earnings/deductions breakdown.
- **Analytics** — department-wise payroll (bar), employee distribution (pie), salary distribution (pie), monthly payroll trend (line) — all via JFreeChart on live data.
- **Reports** — Employee / Payroll / Tax / Department reports, exportable to **PDF**, **Excel (.xlsx)**, and **CSV**.
- **Database** — full MySQL schema (employees, payroll, salary_history, users, departments, tax_slabs, activity_log) with prepared statements everywhere, a HikariCP connection pool, and a DAO layer behind a generic `CrudDAO<T, ID>` interface.
- **UI/UX** — sidebar navigation, gradient login hero, rounded KPI cards, striped tables, hover effects, FlatLaf theming, Light/Dark mode toggle.
- **Extras** — database backup/restore (portable SQL dump, no external `mysqldump` dependency), recent activity log, profile page, about page, keyboard-shortcut reference.
- **OOP demonstrated throughout** — encapsulation (model getters/setters), interfaces + polymorphism (`TaxCalculationStrategy` / `SlabBasedTaxCalculator`), abstraction (`CrudDAO<T,ID>`), custom exceptions (`ValidationException`, `AuthenticationException`, `DataAccessException`), collections, records, and a clean package structure.

## 2. What's intentionally lighter-weight

This is a large brief (the kind of thing real HRMS products spend months on). To keep every included feature genuinely functional rather than stubbed, a few items are simplified:

- **Backup/Restore** works by dumping/replaying SQL `INSERT` statements over JDBC (fully self-contained, no `mysqldump` binary required) rather than a full binary MySQL dump.
- **Tax slabs** are edited as a full-table replace-on-save (simple and safe) rather than granular per-row PATCH.
- **ER / Class / Use-case diagrams** are not included as image files — the schema (`database/schema.sql`) and package layout below serve as the equivalent reference; you're welcome to generate diagrams from them with a tool like dbdiagram.io or PlantUML.
- **Payroll generated_by** tracking is wired in the activity log but not yet threaded onto the `payroll.generated_by` column value itself.

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven |
| UI | Java Swing + FlatLaf (modern flat theme) |
| Database | MySQL 8 via JDBC (`mysql-connector-j`) |
| Connection Pool | HikariCP |
| Charts | JFreeChart |
| PDF | OpenPDF |
| Excel | Apache POI |
| Password Hashing | jBCrypt |

## 4. Project Structure

```
payroll-tax-calculator/
├── pom.xml
├── database/
│   └── schema.sql              # full DDL + seed data
├── src/main/java/com/hrms/payroll/
│   ├── Main.java                # entry point
│   ├── model/                   # Employee, Department, User, TaxSlab, Payroll, SalaryHistory
│   ├── dao/                     # CrudDAO<T,ID> + per-entity interfaces
│   │   └── impl/                # JDBC implementations (PreparedStatement everywhere)
│   ├── database/                # DatabaseConnection (HikariCP pool, singleton)
│   ├── service/                 # business logic: EmployeeService, PayrollService,
│   │                             #   AuthService, AnalyticsService, TaxCalculationStrategy,
│   │                             #   SlabBasedTaxCalculator, AppContext (DI), AppBootstrap
│   ├── controller/               # thin MVC adapters between UI and services
│   ├── ui/                       # LoginFrame, MainDashboardFrame, feature panels
│   │   └── components/           # RoundedPanel, StatCard, GradientPanel, PrimaryButton
│   ├── reports/                  # SalarySlipGenerator (PDF), ReportGenerator, exporters
│   └── utils/                    # ValidationUtils, PasswordHasher, CurrencyFormatter, UITheme
└── src/main/resources/
    ├── db.properties             # DB connection settings — edit this first
    └── logback.xml
```

## 5. Setup & Installation

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.x running locally (or reachable)

### Steps

1. **Create the database:**
   ```bash
   mysql -u root -p < database/schema.sql
   ```
   This creates the `payroll_hrms` database, all tables, and seed data (5 sample employees, 5 departments, 4 tax slabs).

2. **Configure the connection:**
   Edit `src/main/resources/db.properties`:
   ```properties
   db.host=localhost
   db.port=3306
   db.name=payroll_hrms
   db.user=root
   db.password=your_mysql_password
   ```

3. **Run it:**
   ```bash
   mvn clean compile exec:java
   ```
   or build a runnable fat JAR:
   ```bash
   mvn clean package
   java -jar target/payroll-tax-calculator.jar
   ```

4. **Log in** with the default admin account (auto-created on first run):
   - Username: `admin`
   - Password: `Admin@123`

## 6. Tax Slab Logic

Tax is computed **progressively** — each portion of income is taxed at the
rate for the bracket it falls into (not the whole income at the top
marginal rate). Default seed slabs (annual income):

| Slab | Range | Rate |
|---|---|---|
| 1 | ₹0 – ₹250,000 | 0% |
| 2 | ₹250,001 – ₹500,000 | 5% |
| 3 | ₹500,001 – ₹1,000,000 | 20% |
| 4 | Above ₹1,000,000 | 30% |

Editable live from **Tax Calculator** in the app — changes apply to every
payroll run from that point forward.

## 7. Database Schema (summary)

- `employees` — personal + compensation fields, soft-deleted via `is_active`
- `departments` — department master list
- `users` — HR admin logins (BCrypt-hashed passwords, roles: ADMIN / HR_MANAGER / VIEWER)
- `tax_slabs` — editable progressive tax brackets
- `payroll` — one row per employee per pay period (`YYYY-MM`), unique per (employee, period)
- `salary_history` — audit trail of basic-salary/bonus changes
- `activity_log` — feeds the dashboard's Recent Activity list

Full DDL with comments: [`database/schema.sql`](database/schema.sql).

## 8. Extending This Project

- Swap `SlabBasedTaxCalculator` for a different `TaxCalculationStrategy` implementation (e.g. flat-rate, region-specific) without touching `PayrollService` — that's the point of the interface.
- Add new report types by extending `ReportGenerator` with new header/row builder pairs; the PDF/Excel/CSV export plumbing is already generic.
- Add new dashboard KPIs by extending `AnalyticsService.DashboardSnapshot` and `StatCard` usage in `DashboardPanel`.
- Role-based UI restrictions: `User.Role` already exists (ADMIN/HR_MANAGER/VIEWER) — wire conditional rendering in `MainDashboardFrame` based on `currentUser.getRole()`.

## 9. Future Enhancements (not yet built)

- Multi-currency support
- Employee self-service portal (separate login role)
- Email delivery of salary slips
- Leave & attendance management integration
- REST API layer for a future web/mobile client
- Full binary `mysqldump`-based backup as an alternative to the SQL-insert dump

---

*Built as a demonstration of clean Java architecture: MVC, DAO pattern, interface-driven design, exception handling, and a real persistence layer — not a toy CRUD demo.*
