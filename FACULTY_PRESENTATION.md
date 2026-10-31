# Faculty presentation guide

## 1. Opening: 30 seconds

“Good morning, sir/ma’am. My project is Payroll and Tax Calculator with Persistence and Analytics. It calculates an employee’s gross salary, tax deduction, and net salary, stores the results in a database, and presents payroll information through an interactive dashboard. The goal is to reduce repeated manual calculations and make saved payroll records easier to manage.”

## 2. Technology: 1 minute

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | HTML, CSS, vanilla JavaScript | Forms, tables, charts, filters, and user interactions |
| Backend | Java 17 or newer; built-in HTTP server | Validates requests, calculates payroll, and serves the dashboard and API |
| Database | SQLite | Stores records in the local payroll.db file |
| Database access | JDBC and prepared statements | Connects Java to SQLite and safely passes query parameters |
| Communication | HTTP and JSON | Exchanges data between browser and Java server |
| Build | PowerShell build script; Maven alternative | Compiles Java, packages resources, and runs checks |

“Java serves both the frontend and backend. There is no separate frontend development server. SQLite is suitable for this local application because it stores data in one file and needs no separate database server.”

## 3. Explain the calculation: 1 minute

- Gross pay = monthly salary + bonus.
- Tax amount = gross pay × tax rate.
- Net pay = gross pay − tax amount.

Use this example throughout the demonstration:

| Input or result | Value |
|---|---:|
| Employee | Faculty Demo |
| Monthly salary | 50,000 |
| Bonus | 5,000 |
| Tax percentage entered in UI | 10% |
| Gross pay | 55,000 |
| Tax amount | 5,500 |
| Net pay | 49,500 |

“The UI accepts 10 as 10 percent and converts it to 0.10 for the backend. This project uses a configurable flat tax rate; it does not implement statutory income-tax slabs.”

## 4. Live screen-share demo: 4–5 minutes

1. Open http://127.0.0.1:8000 and sign in.
   Say: “For this demonstration, local login bypass is enabled, so any non-empty username and password works. Normal mode verifies a stored workspace account.”
2. Show the dashboard and briefly identify its summary cards, chart, and records table.
   Say: “These views help inspect payroll totals and the distribution of gross, tax, and take-home amounts.”
3. Create the Faculty Demo record using the example above. Show the live calculation preview before saving.
   Say: “The browser gives an immediate preview. On saving, the server validates the inputs and recalculates the amounts before storing them.”
4. Find the record in the table. If it is missing, clear search/date filters and select its creation month.
5. Refresh the browser and show that the record remains.
   Say: “This demonstrates persistence. Records are stored in SQLite and are fetched again from the backend.”
6. Search for Faculty Demo and show sorting or date filtering.
   Say: “Filters help locate records and update the displayed summaries and charts.”
7. Edit the bonus to 6,000.
   Say: “The updated gross is 56,000, tax is 5,600, and net pay is 50,400. The backend recalculates these derived values.”
8. Open the payslip and point out printing or saving as PDF through the browser. Cancel the print dialog after demonstrating it.
9. Export CSV and explain that the filtered records can be used in a spreadsheet.
10. Demonstrate delete using only the Faculty Demo record. First show the confirmation dialog; cancel if you want to retain it.

Avoid editing or deleting existing personal records during screen sharing. Use fictional demonstration data.

## 5. Explain the request flow: 1 minute

“When I press Save, dashboard.js handles the form. payroll-api.js sends an HTTP request to the Java backend. DashboardServer routes the request, Payroll.calculate validates and calculates the amounts, and PayrollRepository saves them through JDBC. The backend returns JSON, and JavaScript updates the dashboard.”

Browser form → JavaScript API client → Java HTTP server → calculation and validation → JDBC → SQLite → JSON response → updated dashboard.

| Operation | API |
|---|---|
| Read payroll records and summary | GET /api/payroll |
| Create payroll record | POST /api/payroll |
| Update payroll record | PUT /api/payroll/{id} |
| Delete payroll record | DELETE /api/payroll/{id} |

“These implement CRUD: Create, Read, Update, and Delete.”

## 6. Code files to show if asked

| File | Explanation |
|---|---|
| src/main/java/payroll/Main.java | Entry point; selects dashboard or command-line operations |
| src/main/java/payroll/Payroll.java | Calculation formulas, input validation, and summary calculations |
| src/main/java/payroll/PayrollRepository.java | Table creation and database CRUD using JDBC |
| src/main/java/payroll/DashboardServer.java | HTTP routes, frontend resources, and API handling |
| src/main/java/payroll/AuthService.java | Account verification, sessions, logout, and optional local bypass |
| src/main/resources/dashboard.html | Page structure and input forms |
| src/main/resources/dashboard.css | Layout, appearance, responsive design, and print styles |
| src/main/resources/dashboard.js | UI interactions, filters, rendering, and previews |
| src/main/resources/payroll-api.js | Browser communication with backend |

If asked to explain one method, choose Payroll.calculate. Explain validation first, then gross, tax, and net calculations, then the returned Breakdown record.

## 7. Database explanation

“The main table is payroll_records. Each record has an auto-generated primary key, employee name, salary, bonus, gross pay, tax rate, tax amount, net pay, total_pay, and creation timestamp. The primary key identifies the exact record to edit or delete.”

Important details:

- total_pay currently stores the same amount as gross_pay; net_pay is the amount after tax.
- The account is stored separately in workspace_account.
- Month and date filters use record creation time, not a dedicated payroll-period field.
- Data persists in payroll.db. Browser local storage holds the appearance preference, not payroll records.

## 8. Likely faculty questions and short answers

**What is persistence?**
Persistence means data remains stored after refreshing the browser or restarting the application because it is saved in the database.

**Why Java?**
Java provides the backend calculation logic, validation, JDBC database access, and HTTP server in this project.

**Why SQLite instead of MySQL?**
SQLite is simple to run locally and does not require a separate database service. A larger multi-user deployment could use a server database.

**Is this React or Spring Boot?**
No. This version uses vanilla JavaScript and Java’s built-in HTTP server.

**What validation is implemented?**
Employee name must not be blank. Salary and bonus must be finite and non-negative. The backend tax rate must be between 0 and 1, corresponding to 0–100 percent.

**Why validate on the server if the browser validates?**
Browser checks improve usability, but requests can be sent directly to the API. The server must validate independently and calculate the stored results itself.

**What are prepared statements?**
They pass values separately from SQL query structure. The repository uses them for insert, update, and delete operations, helping prevent SQL injection through those parameters.

**How does authentication work?**
Normal mode has one local workspace account, with a salted PBKDF2 password hash and expiring sessions. It also checks CSRF tokens for mutations. The current presentation mode deliberately bypasses credential verification; any entered credentials gain access to the same workspace. It does not create separate users or accounts.

**What analytics are available?**
The dashboard shows filtered summaries and payroll charts. The Java summary method calculates record count, total gross, total tax, average net pay, and the employee attached to the largest gross-pay record.

**How was it tested?**
The build runs Java calculation, validation, persistence, HTTP, and security checks. Frontend checks cover logic and page interactions against a Java server. The latest build passed. The frontend interaction suite uses a DOM test double; it is not a full visual browser test.

**What are its limitations?**
It uses a flat tax rate, one local workspace account, and creation dates rather than explicit payroll periods. Monetary calculations currently use double precision. A production payroll system would need decimal money calculations, statutory tax rules, stronger user and role management, and an audit trail.

**What would you add next?**
Tax slabs, deductions such as PF, explicit payroll periods, role-based access, audit history, and scheduled backups.

## 9. Before the meeting

- Start the server and open the dashboard before sharing your screen.
- Keep this guide on another device or outside the shared window.
- Increase browser zoom if text is small; close unrelated tabs and notifications.
- Use fictional data and rehearse the 50,000 + 5,000 example once.
- Keep Payroll.java and PayrollRepository.java ready in the editor.
- If port 8000 is occupied, use --port 8001 and open the corresponding URL.
- If records seem missing, check the month selector and clear search/date filters.

To restart in the current demo mode from PowerShell:

```powershell
java --enable-native-access=ALL-UNNAMED "-Dpayroll.localLoginBypass=true" -cp "build/payroll.jar;lib/*" payroll.Main --dashboard
```

To use normal account verification, omit "-Dpayroll.localLoginBypass=true".

## 10. Closing: 20 seconds

“This project demonstrates payroll calculation, database persistence, CRUD operations, and interactive analytics in one local application. It connects a browser interface to a Java backend and SQLite database. Future improvements would focus on statutory tax rules, more precise money calculations, and multi-user administration.”
