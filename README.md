# CampusFix - Campus Complaint Management System

A console-based Java application for reporting, routing and resolving campus complaints -
Wi-Fi outages, electrical faults, hostel issues, broken equipment and the rest - with a full
status history, role-based access, and basic analytics for administrators.

## What it does

Students file a complaint and get back a unique ID (`CMP-2026-00001`). An admin routes it to a
department, either automatically based on its category or by hand, and assigns it to a staff
member. That staff member accepts it, works it, and resolves it with a written remark. The
student can then track progress at any point, see the complete history of what changed and when,
and leave feedback once it's resolved. Every status change is logged, both to the database and to
a local audit file.

## Tech stack

- Java 17+
- MySQL 8 (via JDBC)
- Maven
- No frameworks - plain console I/O, JDBC and `java.nio.file`

## Project layout

```
CampusFix/
├── src/main/java/com/campusfix/
│   ├── model/         entities: User (abstract) -> Student/Staff/Admin, Complaint, etc.
│   ├── service/        business logic: ComplaintService, AssignmentService, FeedbackService, AnalyticsService
│   ├── dao/             JDBC data access
│   ├── exception/     custom checked exceptions
│   ├── util/            DB connection, validation, file/report handling, logging, ID generation
│   ├── concurrency/  the concurrent-submission demo task
│   └── Main.java       console entry point
├── database/campusfix.sql   schema + seed data
├── data/                          created at runtime: audit log, CSV backup, generated reports
├── pom.xml
└── statement.md
```

## 1. Prerequisites

Install these before doing anything else:

- **JDK 17 or newer** - check with `java -version`
- **Maven 3.6+** - check with `mvn -version`
- **MySQL 8** (a local server is fine) - check with `mysql --version`

If you don't have Maven installed, either install it, or compile/run directly with `javac`/`java`
as shown in the "without Maven" section below.

## 2. Set up the database

From the project root, load the schema and seed data into MySQL:

```bash
mysql -u root -p < database/campusfix.sql
```

This creates a `campusfix` database with all six tables and seeds a handful of accounts you can
log in with right away:

| Role    | Email                  | Password  |
|---------|-------------------------|-----------|
| Admin   | admin@campusfix.edu     | admin123  |
| Student | aditi@campusfix.edu     | pass123   |
| Student | rohan@campusfix.edu     | pass123   |
| Student | meera@campusfix.edu     | pass123   |
| Staff   | staff102@campusfix.edu  | staff123  |
| Staff   | staff201@campusfix.edu  | staff123  |
| Staff   | staff301@campusfix.edu  | staff123  |

You can also register new student/staff accounts from the app itself.

## 3. Configure the database connection

The app reads its connection details from three environment variables, falling back to sensible
local defaults (`root` / `root` on `localhost:3306`) if they aren't set:

```bash
export CAMPUSFIX_DB_URL="jdbc:mysql://localhost:3306/campusfix?useSSL=false&serverTimezone=UTC"
export CAMPUSFIX_DB_USER="root"
export CAMPUSFIX_DB_PASSWORD="your_mysql_password"
```

On Windows (PowerShell):

```powershell
$env:CAMPUSFIX_DB_URL = "jdbc:mysql://localhost:3306/campusfix?useSSL=false&serverTimezone=UTC"
$env:CAMPUSFIX_DB_USER = "root"
$env:CAMPUSFIX_DB_PASSWORD = "your_mysql_password"
```

If you'd rather not use environment variables, edit the fallback values directly in
`src/main/java/com/campusfix/util/DBConnection.java`.

## 4. Build and run (with Maven)

```bash
mvn clean package
java -jar target/campusfix.jar
```

`mvn clean package` compiles everything and bundles the MySQL driver into the jar, so the
`java -jar` step needs nothing else on the classpath.

For quicker iteration while developing, you can skip packaging entirely:

```bash
mvn compile exec:java
```

## 5. Build and run (without Maven)

If Maven isn't available, download the MySQL Connector/J jar (`mysql-connector-j-8.3.0.jar`) and
compile/run manually:

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
java -cp "out:mysql-connector-j-8.3.0.jar" com.campusfix.Main   # use ; instead of : on Windows
```

## 6. Using the app

On launch you'll see a login/register menu. Log in with one of the seeded accounts above (or
register your own), and you'll land on a menu specific to your role:

- **Student**: submit a complaint, track your complaints, view history, leave feedback, cancel a
  pending complaint.
- **Staff**: see what's assigned to you, accept it, put it on hold/resume it, resolve it, view
  history.
- **Admin**: view every complaint sorted by priority, assign departments/staff, change priority,
  view analytics, generate a report + CSV backup, and run a demo that submits several complaints
  concurrently (via `ExecutorService`) to show the thread-safe ID generation in action.

A typical end-to-end run: log in as a student, submit a Wi-Fi complaint, log out, log in as
`admin@campusfix.edu` and auto-assign it (routes to IT Support), assign it to `Staff102`, log in
as that staff account, accept the complaint (moves to `IN_PROGRESS`), resolve it, then log back in
as the student to view the resolution and leave a rating.

## 7. Where things get written

- `data/audit.log` - a line per significant action (login, complaint created, status changed, etc.)
- `data/complaints_backup.csv` - generated on demand from the admin menu
- `data/reports/analytics_report_<timestamp>.txt` - generated on demand from the admin menu

All three are created automatically on first run and are excluded from version control via
`.gitignore`.

## Testing

There's a manual test script in `statement.md`'s companion design docs covering the standard
cases: empty title/description, an unknown category, looking up a missing complaint ID, a
student trying to change status directly, an invalid transition (e.g. `RESOLVED -> SUBMITTED`),
feedback submitted before resolution, and a duplicate submission. Each of these is backed by a
specific custom exception (see `com.campusfix.exception`) so the failure is always caught and
reported cleanly rather than crashing the app.

## Future enhancements

- A real notification layer (email/SMS) instead of console output only
- A web or mobile front end over the same service layer
- AI-assisted category suggestion from the complaint description
- Integration with the university's ERP system
