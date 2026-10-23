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

## Project layout

- `src/main/java/payroll/`: calculations, repository, CLI, and HTTP server
- `src/main/resources/dashboard.html`: browser dashboard bundled in the jar
- `src/test/java/payroll/PayrollTest.java`: automated regression checks
- `pom.xml`: Maven build
- `build.ps1`: build and test without Maven

Python sources and packaging have been replaced. Old local virtual environments
and caches are unused and excluded from Git.

## Offline checks

If the SQLite driver cannot be downloaded, calculations and JSON serialization
can still be compiled and checked without dependencies:

```powershell
New-Item -ItemType Directory -Force build/classes, build/test-classes | Out-Null
javac --release 17 -d build/classes src/main/java/payroll/*.java
javac --release 17 -cp build/classes -d build/test-classes src/test/java/payroll/PayrollTest.java
java -cp "build/test-classes;build/classes" payroll.PayrollTest --core-only
```

This mode does not run SQLite or HTTP integration tests. Use `build.ps1` or
`mvn verify` with the driver available to run the full suite.
