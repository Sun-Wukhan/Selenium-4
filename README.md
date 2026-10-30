# Selenium 4 — Weather Shopper test framework

[![CI](https://github.com/Sun-Wukhan/Selenium-4/actions/workflows/ci.yml/badge.svg)](https://github.com/Sun-Wukhan/Selenium-4/actions/workflows/ci.yml)

A Selenium 4 / TestNG framework in Java 17 that tests the
[Weather Shopper](https://weathershopper.pythonanywhere.com/) demo shop.
The live suite buys moisturisers and checks the cart against the catalogue:
the cheapest product's catalogue price must match its cart line, and the
cheapest Aloe + Almond pair must sum to the cart total.

Around that suite sits a `utilities` module of reusable framework helpers —
browser management, Extent reporting, Excel and database data providers,
email notifications and screen recording. The current tests only exercise
some of them; the rest are wired up as runnable examples under
`client/src/test/java/examples/` (see [Utilities](#utilities) below).

## Stack

| Piece | Version / note |
| --- | --- |
| Java | 17 |
| Selenium | 4.51.0, drivers and browsers resolved by Selenium Manager — no manual driver downloads |
| TestNG | 7.10.2, suite defined in `client/src/test/java/runner/buy.xml` |
| Build | Maven multi-module (`client` + `utilities`), Surefire 3.5.2 |
| Reporting | Extent Reports 5.1.2 (Spark HTML) |
| Data | Apache POI 5.4.0 (Excel), PostgreSQL JDBC 42.7.4 |
| Extras | Apache Commons Email, Monte Screen Recorder 0.7.7.0 |

## Quick start

You need JDK 17 and Maven — or just Docker.

```bash
make test-headless   # headless Chrome, the CI way
make test            # Chrome with a visible window
make test-firefox    # Firefox (also: test-edge, *-headless variants)
make report          # open the Extent HTML report from the last run
make docker-test     # build the image and run the suite inside Docker
```

The plain Maven equivalent:

```bash
mvn -pl client -am test -Dheadless=true -Dbrowser=chrome
```

The report lands at `client/target/extent-report/index.html`, with a
pass/fail screenshot attached to every test.

## Configuration

Set as Maven system properties (the `Makefile` variables map to them):

| Property | Default | Values |
| --- | --- | --- |
| `url` | `https://weathershopper.pythonanywhere.com/moisturizer` | any catalogue URL |
| `browser` | `chrome` | `chrome`, `firefox`, `edge` |
| `headless` | `false` | `true` / `false` |

```bash
mvn -pl client -am test -Dbrowser=firefox -Dheadless=true
make test BROWSER=edge URL=https://weathershopper.pythonanywhere.com/sunscreen
```

## Project structure

```
.
├── pom.xml                  # parent POM: versions, dependency management
├── client/                  # the tests
│   └── src/test/java/
│       ├── page_objects/        # MoisturizerPage, CartPage, CatalogProduct
│       ├── page_objects_test/   # PurchaseTest — the live suite
│       ├── application_page_actions/  # NavBot (waits/clicks/cookies), Clank (AWT keys)
│       ├── examples/            # utility usage examples (below)
│       └── runner/buy.xml       # TestNG suite file Surefire runs
├── utilities/               # the reusable framework module
│   └── src/main/java/
│       ├── webmanager/      # WebManager — browser lifecycle + reporting hooks
│       ├── reporting/       # ExtentManager, ExtentTestManager, TestLogger, FileLogger
│       ├── excelreader/     # MyDataReader — .xlsx → String[][]
│       ├── database_utilities/  # DBUtilities, DBDataProvider — PostgreSQL → String[][]
│       ├── email/           # SendingEmail — failure mail, attachments, HTML mail
│       └── screenrecording/ # MyScreenRecorder — Monte .avi capture
├── Dockerfile               # Maven + Chrome image that runs the suite headless
└── Makefile                 # test / docker / report targets
```

## How a test runs

Every test class extends `WebManager`:

1. `@BeforeSuite` creates the shared Extent report.
2. `@BeforeMethod` starts a browser through Selenium Manager (ThreadLocal,
   so parallel methods don't share a driver), opens the configured URL and
   starts an Extent test entry for the method.
3. The test works through page objects — locators and page behaviour live in
   `page_objects/`, assertions live in the test.
4. `@AfterMethod` attaches a pass/fail screenshot to the report and quits
   the browser; `@AfterSuite` flushes the HTML report.

`NavBot` holds the low-level actions page objects build on — explicit-wait
clicks, `sendKeys`, dropdown selection, cookie save/load, page-load waits.
`PurchaseTest` is the template to copy for a new test.

## Utilities

| Utility | What it does | Status in this repo |
| --- | --- | --- |
| `webmanager.WebManager` | Browser lifecycle, screenshots, Extent hooks | Used by every test |
| `reporting.TestLogger` / `ExtentManager` / `ExtentTestManager` | Step logging into TestNG + Extent; the shared report session | Used via `WebManager` and `NavBot` |
| `reporting.FileLogger` / `ApplicationLog` | Plain-text file logging; epic-level log banner | Available |
| `excelreader.MyDataReader` | Reads an `.xlsx` sheet into `String[][]`, header row skipped — a ready-made TestNG data provider source | Example below |
| `database_utilities.DBDataProvider` / `DBUtilities` | Runs PostgreSQL queries and returns rows as `String[][]` for a data provider | Example below |
| `email.SendingEmail` | Simple failure email, email with attachment, HTML email with inline image (Commons Email) | Example below |
| `screenrecording.MyScreenRecorder` | Records the desktop to `.avi` during a test (Monte) | Example below |
| `application_page_actions.Clank` | AWT `Robot` helpers — clipboard file upload, key presses, full-screen capture for native dialogs Selenium can't reach | Available |

### Examples

Complete, compilable examples live in `client/src/test/java/examples/`.
They are not in `buy.xml`, so they don't change what the suite runs; add a
class to the suite file to run one.

- **`ExcelDrivenPurchaseExample`** — the Aloe/Almond purchase test driven by
  an Excel sheet through `MyDataReader`. Each row of
  `src/test/resources/data/ingredients.xlsx` runs the test once (a sample
  workbook is seeded on first run; replace it with a committed sheet).
- **`DatabaseDrivenExample`** — a `@DataProvider` backed by
  `DBDataProvider.dataSupplier(...)` over the `public.testdata` table.
  Disabled until a database exists: create the table and point
  `DBUtilities.loadProperties()` at your own `app.properties`
  (`url.test`, `userName.test`, `password.test`) — as committed it reads a
  hardcoded path on the author's machine.
- **`ScreenRecordingExample`** — wraps a test in `MyScreenRecorder`
  start/stop; the `.avi` files in this repo's first commit were made this
  way. Headed, local runs only — a screen recorder can't capture a headless
  browser, so it stays disabled in CI.
- **`FailureEmailListenerExample`** — a TestNG `ITestListener` that calls
  `SendingEmail.sendSimpleEmail(...)` on failure. Register it with
  `@Listeners` or in `buy.xml`, and replace the placeholder SMTP
  credentials/recipients in `SendingEmail` first. Send failures are logged,
  never allowed to fail a test.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `master`
and every pull request:

- **test** — JDK 17 on Ubuntu, runs the suite headless in Chrome exactly as
  `make test-headless` does, and uploads the Extent report and Surefire
  results as an artifact, pass or fail.
- **docker** — builds the `Dockerfile` image and runs the suite inside it
  (`make docker-test`).

## Notes

- The suite tests a live third-party demo site; a red build can mean the
  site changed or is down, not that the framework broke. Check the Extent
  artifact's screenshots first.
- `client/recordings/` and `client/screenshots/` in the git history are
  captures from the original 2023 runs. Fresh captures are gitignored.
