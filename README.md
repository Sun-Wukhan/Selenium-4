# Selenium 4 — Weather Shopper test framework

[![CI](https://github.com/Sun-Wukhan/Selenium-4/actions/workflows/ci.yml/badge.svg)](https://github.com/Sun-Wukhan/Selenium-4/actions/workflows/ci.yml)

A Selenium 4 / TestNG framework in Java 17 that tests the
[Weather Shopper](https://weathershopper.pythonanywhere.com/) demo shop end
to end. The live suite (34 test invocations, run in parallel) covers the home page's
temperature rule (below 19° buy moisturisers, above 34° buy sunscreens),
both catalogues against the cart — cheapest Aloe + Almond moisturisers,
cheapest SPF-30 + SPF-50 sunscreens — a full checkout that pays with a
Stripe test card and verifies the confirmation page, cart edge cases
such as the empty cart, seeded property-style generated purchase cases,
axe-core accessibility checks, and visual layout checks. The latest Extent report from `master` is published
at [sun-wukhan.github.io/Selenium-4](https://sun-wukhan.github.io/Selenium-4/).

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
| Build | Maven multi-module (`client` + `utilities` + `api`), Surefire 3.5.2 |
| API testing | REST Assured 5.5.7 + jsoup, TestNG suite in `api/src/test/resources/testng-api.xml` |
| Reporting | Extent Reports 5.1.2 (Spark HTML) |
| Accessibility | axe-core 4.10.1 (Deque `axe-core:selenium`) |
| Data | Apache POI 6.0.0 (Excel), PostgreSQL JDBC 42.7.14 |
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

The API suite needs the shop itself. Run the shop locally (it is the
open-source [qxf2/weather-shopper](https://github.com/qxf2/weather-shopper)
Flask app, port 6464), then the contract tests:

```bash
git clone https://github.com/qxf2/weather-shopper && cd weather-shopper
python3 -m venv .venv && .venv/bin/pip install flask && .venv/bin/python weather_shopper.py &
mvn -pl api test   # 17 contract tests against http://127.0.0.1:6464
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
| `retry.max` | `1` | extra attempts per failed test before it settles as a failure |
| `property.seed` | `20261103` | seed for the generated cases in `PropertyPurchaseTest` |
| `shop.baseUrl` | `http://127.0.0.1:6464` | base URL of the shop the API suite tests (`api` module) |

```bash
mvn -pl client -am test -Dbrowser=firefox -Dheadless=true
make test BROWSER=edge URL=https://weathershopper.pythonanywhere.com/sunscreen
```

## API tests

The `api` module tests the shop's backend directly over HTTP with REST
Assured — no browser — so a failure says whether the shop or the UI
layer broke. The shop is server-rendered (pages and form posts, no
JSON), and the tests pin the contracts the UI suite silently depends on:

| Test class | Contract |
| --- | --- |
| `HomeApiTest` | Home page served as HTML; the temperature script carries both decisive bands (1–18, 35–49) |
| `CatalogApiTest` | Each catalogue page is exactly 6 products sampled from the canonical pool at pool prices; moisturizer pages always offer both Aloe and Almond; sunscreen names carry their SPF rating |
| `CartApiTest` | The cart lists the submitted items and totals them — including that it totals the *posted* prices |
| `ConfirmationApiTest` | Every confirmation renders exactly one outcome; at least 30 of 40 succeed (the designed rate is 95%, so the bound cannot flake) |
| `RoutingApiTest` | `/about` serves; unknown paths are 404; catalogues reject POST and `/confirmation` rejects GET (405) |

Run it against a locally hosted shop (see Quick start), or point it at
any deployment with `-Dshop.baseUrl`:

```bash
mvn -pl api test
mvn -pl api test -Dshop.baseUrl=https://weathershopper.pythonanywhere.com
```

Example run, captured from CI (the "API suite (REST Assured)" job):

```text
$ mvn -B -pl api test
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.662 s -- in TestSuite
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  4.675 s
```

The whole backend contract executes in under two seconds of test time —
against the same shop instance the browser suite is testing, in the
same CI run.

Every run also produces an HTML report in the same style as the
browser suite's: `api/target/api-report/index.html` lists each test
with its description, outcome and duration, plus the HTTP exchanges it
made — method, path, status, elapsed time and a response excerpt —
captured by a REST Assured filter (`api.reporting`). On pushes to
`master` it is published alongside the UI report at
[sun-wukhan.github.io/Selenium-4/api](https://sun-wukhan.github.io/Selenium-4/api/).

### Performance tests

The `api` module also carries a Java performance layer (package
`api.perf`): a small dependency-free load harness drives the endpoints
concurrently (12 threads, warm-up excluded) and reports mean, p50, p95,
p99 and max latency plus throughput per workload. Each test asserts
zero errors and a p95 under its budget (750 ms by default) — the budgets
are regression tripwires, not load-test heroics, and every value is
overridable (`-Dperf.threads`, `-Dperf.requests`, `-Dperf.p95.*`). The
suite lives behind the `perf` Maven profile so ordinary builds and PR
gates run only the deterministic contract tests:

```bash
mvn -pl api test -Pperf   # shop must be running, as above
```

Every run appends its table to `api/target/perf-report.md`. Measured in
CI against the self-hosted shop (PR #18, November 2026):

| Workload | Requests | Errors | Mean | p95 | p99 | Throughput |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| GET /moisturizer | 216 | 0 | 27 ms | 41 ms | 45 ms | 114 req/s |
| GET /sunscreen | 216 | 0 | 26 ms | 34 ms | 40 ms | 392 req/s |
| POST /cart | 216 | 0 | 26 ms | 37 ms | 43 ms | 382 req/s |
| Mixed workload (70/20/10) | 276 | 0 | 23 ms | 29 ms | 42 ms | 475 req/s |

The same numbers are rendered as a styled page at
`api/target/perf-report.html` — summary cards, per-workload latency
detail and p95-versus-budget bars — published on `master` pushes at
[sun-wukhan.github.io/Selenium-4/perf](https://sun-wukhan.github.io/Selenium-4/perf/).

## Project structure

```
.
├── pom.xml                  # parent POM: versions, dependency management
├── client/                  # the tests
│   └── src/test/java/
│       ├── page_objects/        # HomePage, MoisturizerPage, SunscreenPage, CartPage, ConfirmationPage
│       ├── page_objects_test/   # the live suite: purchase, sunscreen, home, checkout, data-driven, property, accessibility, visual, edge cases
│       ├── application_page_actions/  # NavBot (waits/clicks/cookies), Clank (AWT keys)
│       ├── examples/            # utility usage examples (below)
│       └── runner/buy.xml       # TestNG suite file Surefire runs
├── utilities/               # the reusable framework module
│   └── src/main/java/
│       ├── webmanager/      # WebManager — browser lifecycle + reporting hooks; RetryAnalyzer + transformer
│       ├── reporting/       # ExtentManager, ExtentTestManager, FlakeTally, TestLogger, FileLogger
│       ├── excelreader/     # MyDataReader — .xlsx → String[][]
│       ├── database_utilities/  # DBUtilities, DBDataProvider — PostgreSQL → String[][]
│       ├── email/           # SendingEmail — failure mail, attachments, HTML mail
│       └── screenrecording/ # MyScreenRecorder — Monte .avi capture
├── api/                     # the backend contract suite (REST Assured)
│   └── src/test/
│       ├── java/api/
│       │   ├── config/      # ApiConfig — shop base URL + endpoint paths
│       │   ├── client/      # ShopClient — the REST Assured calls
│       │   ├── data/        # ShopCatalog — the shop's canonical product pools
│       │   ├── model/       # Product
│       │   ├── parsing/     # CatalogPageParser — jsoup, same selectors as the page objects
│       │   ├── perf/        # LoadHarness, budgets, perf tests (perf profile only)
│       │   ├── reporting/   # ExchangeCapture filter + Extent report listener
│       │   └── tests/       # Home, Catalog, Cart, Confirmation, Routing contract tests
│       └── resources/       # testng-api.xml (contract) and testng-perf.xml (performance)
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

## Retries, flakes and generated cases

The shop is deliberately non-deterministic — it re-randomises its
catalogue on every load and fails about 5% of payments by design — so
the framework treats a single failure as weak evidence:

- **Retry analyzer** — a failed test is retried up to `-Dretry.max`
  times (default 1) before TestNG settles it as a failure.
  `RetryAnnotationTransformer`, registered in `buy.xml`, applies the
  analyzer to every `@Test`, so no class declares it.
- **Flake tally** — retried attempts are not hidden. `FlakeTally`
  publishes a tally to the Extent report (system info plus its own
  entry) separating *flaky passes* (green only after a retry — a
  stability problem) from *hard failures* (still red after every
  attempt — a bug).
- **Property-style tests** — `PropertyPurchaseTest` generates its
  cases from a seed (`-Dproperty.seed`, default `20261103`) instead of
  hand-picking two: random subsets of moisturiser ingredients and
  sunscreen ratings, each held to the same invariants (the cart lists
  exactly what was chosen, at catalogue prices, totalling their sum).
  A failing case reproduces by rerunning with the same seed.
- **Accessibility** — `AccessibilityTest` runs axe-core (WCAG 2 A/AA)
  on the home, catalogue and cart pages. Critical violations fail the
  build, with one documented acceptance: the shop's product images
  ship without alt text (`image-alt`, six per catalogue page), which
  is reported as a warning instead — the shop is a third-party demo
  this suite does not control. Lesser violations are warnings too.
- **Visual checks** — `VisualLayoutTest` checks layout geometry
  (exactly six cards per load, no overlaps, Add buttons inside their
  cards) and that screenshots are real, non-blank renders. Pixel-diff
  baselines would flake by design here: the catalogue changes on every
  load, so no two screenshots ever match.

## Utilities

| Utility | What it does | Status in this repo |
| --- | --- | --- |
| `webmanager.WebManager` | Browser lifecycle, screenshots, Extent hooks | Used by every test |
| `reporting.TestLogger` / `ExtentManager` / `ExtentTestManager` | Step logging into TestNG + Extent; the shared report session | Used via `WebManager` and `NavBot` |
| `reporting.FileLogger` / `ApplicationLog` | Plain-text file logging; epic-level log banner | Available |
| `excelreader.MyDataReader` | Reads an `.xlsx` sheet into `String[][]`, header row skipped — a ready-made TestNG data provider source | Used by `DataDrivenPurchaseTest`; example below |
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

- **test** — JDK 17 on Ubuntu, a browser matrix (Chrome, Firefox, Edge,
  all headless) running the suite exactly as `make test-headless` does, and
  uploading the Extent report and Surefire results as an artifact per
  browser, pass or fail. The job first starts a self-hosted copy of the
  shop (checked out from qxf2/weather-shopper) and points the suite at it
  with `-Durl=http://127.0.0.1:6464/moisturizer`, so CI no longer depends
  on the public demo site's uptime or latency.
- **api** — starts the same self-hosted shop and runs the REST Assured
  contract suite (`mvn -pl api test`): catalogue sampling invariants,
  cart totals, the confirmation coin-flip contract, routing and status
  codes — the backend half the browser cannot see.
- **perf** — starts the same self-hosted shop and runs the performance
  suite (`mvn -pl api test -Pperf`), publishing the generated
  `perf-report.md` to the run summary and as an artifact. The workflow
  also accepts manual runs (`workflow_dispatch`) for on-demand checks.
- **docker** — builds the `Dockerfile` image and runs the suite inside it
  (`make docker-test`); this job still targets the live demo site.
- **report** — on pushes to `master` only, publishes all three reports
  to GitHub Pages: the Chrome run's Extent report at
  [sun-wukhan.github.io/Selenium-4](https://sun-wukhan.github.io/Selenium-4/),
  the API report at `/api/`, and the performance report at `/perf/`.
- **triage** — runs only when the test job fails. Downloads the Surefire
  reports and runs `ci/ai-triage.py`, which asks an LLM for a root-cause
  summary per failure and publishes it to the run's step summary and an
  `ai-triage` artifact. Enable it by adding an `ANTHROPIC_API_KEY` or
  `OPENAI_API_KEY` repository secret (optionally `AI_TRIAGE_MODEL` as an
  Actions variable to pick the model); with no key configured it posts the
  raw failure excerpts instead, and it never fails the build itself.

Within a run, the suite itself executes with TestNG `parallel="methods"`
and two threads (`runner/buy.xml`); `WebManager`'s ThreadLocal drivers
keep parallel methods in separate browsers. Dependabot watches the Maven,
GitHub Actions and Docker dependencies weekly.

## Notes

- In CI the suites run against a self-hosted copy of the shop, so a red
  build there is a real signal. Local UI runs still default to the live
  demo site (pass `-Durl` to change that); a red local run can mean the
  site changed or is down. Check the Extent artifact's screenshots first.
- The shop fails about 5% of payments at random, by design (its
  confirmation page says so). `CheckoutTest` treats a `PAYMENT FAILED`
  result as the designed flake it is and shops + pays once more before
  failing. The failure page itself cannot be forced: the shop's
  `/confirmation` endpoint never charges the card — it renders success or
  failure from a weighted coin flip — so `CheckoutFailureTest` covers the
  deterministic failure surface instead, the Stripe overlay's own
  validation (a card number with a bad checksum, an expiry in the past),
  which rejects the details and never submits a payment.
- `DataDrivenPurchaseTest` runs the purchase checks once per row of
  `src/test/resources/data/ingredients.xlsx` (sheets `Ingredients` and
  `Sunscreens`). Commit a sheet at that path to control the data; when
  none exists, a sample workbook is seeded on first run.
- Captures from the original 2023 runs (`.idea/`, `client/recordings/`,
  `client/screenshots/`) were removed from the tree; they remain in the git
  history. Fresh captures are gitignored.
