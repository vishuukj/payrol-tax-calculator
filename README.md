# Payroll & Tax Calculator + Persistence & Analytics

Java 17+ payroll application with SQLite storage, analytics, a CLI, and a browser dashboard.

## Build and test on Windows

Install JDK 17 or newer, then run from this project folder:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

The script downloads SQLite JDBC from Maven Central to `lib/`, compiles
Java sources, packages `build/payroll.jar`, and runs calculation, validation,
persistence, analytics, and HTTP integration tests. Internet access is required
for the first build. Dependencies are cached for subsequent builds.

## Run

```powershell
java -cp "build/payroll.jar;lib/*" payroll.Main --help
java -cp "build/payroll.jar;lib/*" payroll.Main --employee Alice --salary 5000 --bonus 250 --tax-rate 0.2
java -cp "build/payroll.jar;lib/*" payroll.Main --list
java -cp "build/payroll.jar;lib/*" payroll.Main --summary
java -cp "build/payroll.jar;lib/*" payroll.Main --tax-only 5250 --tax-rate 0.2
java -cp "build/payroll.jar;lib/*" payroll.Main --dashboard
```

Open http://127.0.0.1:8000 for the dashboard. Stop with Ctrl+C.
Use `--port 9000` to change the port and `--db path/to/file.db` to choose a database.
The database parent directory must exist. On macOS/Linux, use a colon instead
of a semicolon in the classpath.

The Alice example produces gross pay 5250, tax 1050, and net pay 4200.
Tax rates are fractions between 0 and 1. Salary and bonus must be finite and
non-negative. Calculations use double precision; analytics round to two decimal
places. Tax is a configurable flat rate.

## Maven alternative

```sh
mvn verify
mvn exec:java -Dexec.args="--employee Alice --salary 5000 --bonus 250"
mvn exec:java -Dexec.args="--dashboard"
```

## Existing data

The Java application uses the original `payroll_records` SQLite schema. Run from
this directory to continue using the existing `payroll.db`, or pass its path
with `--db`. Databases are excluded from Git because they can hold employee data.
Tests use a separate temporary database and do not modify your payroll records.

## Interactive dashboard

The web frontend lives in `src/main/resources/` and uses HTML, CSS, and vanilla
JavaScript, served by `payroll.DashboardServer`. The Swing/MySQL application on
the repository's `main` branch is separate and has not been changed by this UI
redesign. This web application uses the existing Java/SQLite HTTP API.

The interface includes a navy sidebar, responsive drawer navigation, a month
selector, light/dark/system appearance settings, four filtered summary cards,
and a live take-home/tax stacked bar. The month selector filters by record
creation month, because the current schema has no separate payroll period.
No currency code is configured; the UI preserves the existing unitless amounts
and flat-rate tax rules instead of introducing an illustrative dollar currency.

To build and run this version alongside an older running jar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -JarPath build/payroll-studio.jar
java --enable-native-access=ALL-UNNAMED -cp "build/payroll-studio.jar;lib/*" payroll.Main --dashboard --port 8001
```

Open <http://127.0.0.1:8001/>. Stop any existing server on that port before starting
another. Java serves both the frontend and backend, so there is no separate npm
development server. The PowerShell build runs frontend checks when Node is
installed. Browser layout, print-dialog, and device testing still require a
browser; Node checks exercise frontend logic using a small DOM test double.

Data access is isolated in `payroll-api.js`. There is no local-storage fallback
or demo record mode: successful writes must receive a response from the backend.
Only the appearance preference uses local storage; authentication uses an
HttpOnly session cookie. Records and passwords are not saved in browser storage.
The existing SQLite
backend needs no further endpoints for these controls. Connecting this UI to
the separate desktop MySQL database would require an HTTP adapter for that
application; it is not currently connected to that database.

- Create payroll with a live gross, tax, and net preview. The form accepts tax as
  a percentage (20 means 20%); the backend accepts the fractional rate (0.2).
- Search employees, sort monthly salary, and filter by creation date (inclusive).
  Summary cards and charts reflect the filtered records.
- Edit records in a modal or permanently delete them after confirmation.
- Switch charts between individual payroll records and monthly totals. Toggle
  gross/net series and hover or focus bars to inspect values.
- Toggle light/dark mode; the preference is saved in the browser.
- Export the filtered records as CSV, or open a record's Slip action and print
  it or save it as PDF using the browser's print dialog.

The dashboard is a local application served on the loopback interface. On first
visit, create your workspace account with a username and a password of at least
8 characters. There is no default password. The account is stored in the same
SQLite database using a salted PBKDF2 password hash. The avatar and sidebar
account button open the account menu and Sign out action.

All payroll HTTP endpoints require a signed-in session. Sessions use an HttpOnly,
SameSite=Strict cookie and expire after 8 hours or a server restart. Signing out
revokes that session. Login is for one local workspace account, not a multi-user
role/permission system; the separate Swing/MySQL login remains unchanged.
Command-line operations continue to use direct database access as before.

State-changing requests also require the session's `X-CSRF-Token` header. The
frontend manages this automatically, including setup, login, and logout. Custom
HTTP clients must first call `/api/auth/session` with cookie persistence, read
`csrfToken`, and send it on mutations. Login rotates the token; use the value in
the login response afterward. See [SECURITY.md](SECURITY.md) for boundaries,
diagnostic policies, and local deployment limitations.

Refresh fetches current database records without clearing filters and displays
a completion message and update time. API calls time out after 12 seconds with
an actionable error instead of leaving controls loading forever.

### HTTP API

- `GET /api/auth/session`: current session and first-run setup status
- `POST /api/auth/setup`: create the first workspace account
- `POST /api/auth/login`: sign in
- `POST /api/auth/logout`: revoke the current session

Setup/login accept form-encoded `username` and `password` fields. The following
payroll endpoints require the session cookie:

- `GET /api/payroll`: summary and records
- `POST /api/payroll`: create
- `PUT /api/payroll/{id}`: update
- `DELETE /api/payroll/{id}`: permanently delete

POST and PUT accept `application/x-www-form-urlencoded` fields:
`employee_name`, `monthly_salary`, `bonus`, and `tax_rate` (fraction 0–1).
Gross, tax, and net are always recalculated on the server.

## Source files

- `src/main/java/payroll/`: calculations, repository, CLI, and HTTP server
- `src/main/resources/dashboard.html`: browser dashboard bundled in the jar
- `src/main/resources/dashboard.css`: theme tokens, layout, reusable UI styles,
  responsive navigation, and print rules
- `src/main/resources/dashboard.js`: rendering, shared form components, filters,
  navigation, appearance settings, and actions
- `src/main/resources/payroll-api.js`: HTTP requests and form-encoded writes
- `src/test/java/payroll/PayrollTest.java`: automated regression checks
- `pom.xml`: Maven build
- `build.ps1`: build and test without Maven

Frontend logic checks (Node.js, no packages required):

```sh
node src/test/js/dashboard.test.cjs
node src/test/js/page-integration.test.cjs
```

The page integration suite reads the actual page markup and invokes its event
handlers through a strict DOM test double against a real Java server. Unknown
element IDs fail the test. It covers account setup/login/logout, Refresh,
create/edit/delete, deletion cancellation, exports, print initiation, settings,
navigation, and theme controls. It uses a disposable temporary database and
never creates an account or changes records in your real `payroll.db`.

Python sources and packaging have been replaced. Old local virtual environments
and caches are unused and excluded from Git.

## Offline checks

If the SQLite driver cannot be downloaded, calculations and JSON serialization
can still be compiled and checked without dependencies:

```powershell
New-Item -ItemType Directory -Force build/classes, build/test-classes | Out-Null
javac --release 17 -d build/classes src/main/java/payroll/*.java
javac --release 17 -cp build/classes -d build/test-classes src/test/java/payroll/*.java
java -cp "build/test-classes;build/classes" payroll.PayrollTest --core-only
```

This mode does not run SQLite or HTTP integration tests. Use `build.ps1` or
`mvn verify` with the driver available to run the full suite.
