# GameCafeManager

GameCafeManager is a Windows desktop application for running a gaming café. It
manages rentable stations, timed sessions, products and inventory, checkout,
invoices, reporting, local users, and thermal receipt printing.

Made by Ehab Salah.

## Features

- PlayStation rooms, billiard tables, and ping-pong tables
- Single and Multi hourly rates for PlayStation and ping-pong stations
- Persisted session start times, elapsed-time display, and active-session
  recovery after an application restart
- Hourly-rate snapshots, so changing a station price does not affect an
  already-started or historical session
- Product inventory, product sales during sessions, and standalone product
  sales without a session
- Checkout with 0%, 10%, 20%, or 50% discounts applied only to gaming time
- Persisted invoices, invoice preview, and receipt reprinting
- Windows printer selection with 80 mm and 58 mm receipt layouts
- Optional automatic receipt printing after checkout
- Admin and cashier roles, secure local password storage, and remembered login
- Reports for completed sessions, revenue, popular stations/products, and
  average session duration
- Application settings for café name, currency, invoice footer, pricing, and
  receipt printing

## Technology

- Java 11 bytecode target
- JavaFX 13
- Maven
- SQLite with JDBC
- JUnit 5
- `jpackage` and WiX for self-contained Windows releases

## Project structure

```text
src/main/java/com/gamecafe/gamecafemanager/
├── core/           # Database path, navigation, utilities, and validation
├── data/           # SQLite, migrations, DAOs, and repository implementations
├── domain/         # Models, repository contracts, services, and use cases
└── presentation/   # JavaFX views, controllers, view models, and UI services
```

The presentation layer does not execute SQL or calculate pricing. Pricing uses
persisted session start times and captured hourly-rate snapshots, while
repositories isolate SQLite access.

## Development prerequisites

To build or run from source, install:

- A JDK compatible with the Maven configuration
- Maven

JavaFX and SQLite JDBC are Maven dependencies. SQLite does not need a separate
installation.

## Build, test, and run

From the project root:

```powershell
mvn clean verify
```

Run the desktop application during development:

```powershell
mvn javafx:run
```

The complete automated suite is run by `mvn clean verify`.

## Database

The application automatically creates and opens its local SQLite database at:

```text
C:\Users\<WindowsUser>\.game-cafe-manager\game-cafe.db
```

This location is outside the application installation directory, so it remains
writable for a normal Windows user and is preserved across application updates
or reinstallations. Database migrations run automatically and do not recreate
or reset existing customer data.

For isolated development or verification runs, set the
`GAME_CAFE_DATABASE_PATH` environment variable to an alternate database path.

## Windows release

The current release version is **1.2.1**. Build a self-contained Windows app
image and installer with:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\build-release.ps1
```

The generated installer is written to:

```text
release\windows\GameCafeManager-Setup.exe
```

The installer includes a private Java runtime, JavaFX modules, the application,
and SQLite JDBC. Customers do not need Java, JavaFX, Maven, NetBeans, or SQLite
installed on their computer.

For full packaging prerequisites, upgrade behavior, and clean-machine testing,
see [Windows release documentation](packaging/windows/WINDOWS_RELEASE.md).

## Customer installation

1. Copy `GameCafeManager-Setup.exe` to a USB drive.
2. Run it on the customer Windows computer.
3. Complete the installer.
4. Start **GameCafeManager** from the Desktop or Start Menu shortcut.
5. Create the initial administrator account on first launch.

## Important behavior

- Active session timing is calculated from `current time - persisted start time`;
  it never relies on an incrementing counter.
- Gaming discounts never alter product prices or product totals.
- Historical sessions, invoices, receipt reprints, and reports use persisted
  monetary snapshots rather than current station or product prices.
- Receipt printing uses printers installed in Windows. A printer failure never
  reverses a completed checkout.

## Release limitations

- A physical thermal printer still requires its Windows driver to be installed.
- The generated installer is not digitally signed unless a code-signing
  certificate is supplied separately.
- Backup/restore, cloud synchronization, remote databases, and automatic
  updates are not included.
