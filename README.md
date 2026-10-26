# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

> **Project status.** Domain model, business rules (service layer), and the team and
> retrospective endpoints are done and tested. Feedback and action-item endpoints come next. See
> [Current status](#current-status).

## Contents

- [Tech stack](#tech-stack)
- [Getting started (≈5 minutes)](#getting-started-5-minutes)
- [IDE setup](#ide-setup)
- [Running tests](#running-tests)
- [Testing the API manually](#testing-the-api-manually)
- [Configuration](#configuration)
- [Running with Docker](#running-with-docker)
- [CI](#ci)
- [Project structure](#project-structure)
- [Business rules](#business-rules)
- [API endpoints](#api-endpoints)
- [Current status](#current-status)
- [Troubleshooting](#troubleshooting)
- [Contact](#contact)

## Tech stack

|           |                                                                                        |
|-----------|----------------------------------------------------------------------------------------|
| Language  | Java 21                                                                                |
| Framework | Spring Boot 3.3.4 (Web, Data JPA, Validation, Actuator, DevTools)                      |
| Build     | Maven, through the included Maven wrapper (`./mvnw`). You don't need to install Maven. |
| Database  | H2, **in-memory**. Data is lost when the application stops.                            |
| Coverage  | JaCoCo, with an 80% minimum line coverage enforced by `./mvnw verify`                  |

## Getting started (≈5 minutes)

### 1. Install a JDK 21

The only prerequisite is a **JDK 21**, preferably [Eclipse Temurin](https://adoptium.net/).

```sh
java -version   # should print version "21.x"
```

<details>
<summary>Installing JDK 21</summary>

```sh
# macOS (Homebrew)
brew install --cask temurin@21

# Any OS (SDKMAN!)
sdk install java 21-tem   # or pick a 21.x Temurin from `sdk list java`
```

If several JDKs are installed on macOS, select 21 for the current shell:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Newer JDKs (such as 25) currently start the application, but they print warnings and
aren't what CI uses. Use 21 to avoid surprises.
</details>

### 2. Clone and run

```sh
git clone <repository-url> retroflow
cd retroflow
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

The first run downloads Maven and the project dependencies, which takes 1–3 minutes. Later
starts take a few seconds. The application is ready when you see:

```
Started RetroflowApplication in 2.1 seconds
```

### 3. Check that it works

```sh
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

You now have the API running locally. Try the requests in [`http/retroflow.http`](http/retroflow.http),
or see the [API endpoints](#api-endpoints).

### Alternative: dev container

If you use VS Code or GitHub Codespaces, open the repository in the dev container (`.devcontainer/`). It provides Java
21 and Maven, compiles the project, and forwards port 8080.
Then run step 2 inside the container.

## IDE setup

### IntelliJ IDEA

1. **File → Open** and select `pom.xml`, then choose *Open as Project*.
2. **File → Project Structure → Project**: set the SDK to a JDK 21 and the language level to 21.
3. Run `RetroflowApplication` (green arrow next to `main`).
4. EditorConfig is applied automatically from `.editorconfig`.

**Faster feedback with DevTools.** `spring-boot-devtools` restarts the application within
about a second whenever compiled classes change. To make IntelliJ recompile automatically:

- *Settings → Build, Execution, Deployment → Compiler →* ✅ **Build project automatically**
- *Settings → Advanced Settings →* ✅ **Allow auto-make to start even if developed application
  is currently running**

Without these settings, press **Build → Build Project** (⌘F9 / Ctrl+F9) to trigger a restart.
DevTools isn't included in the packaged jar or the Docker image.

### VS Code

Install the **Extension Pack for Java** (`vscjava.vscode-java-pack`). Open the folder, wait for
the Java project to import, then run `RetroflowApplication` from the *Run and Debug* view or
the Spring Boot Dashboard. DevTools restarts the application automatically when you save.

## Running tests

| Command                                                                                 | What it does                                                                                                              |
|-----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `./mvnw test`                                                                           | Compiles and runs all tests                                                                                               |
| `./mvnw test -Dtest=RetroflowBusinessRulesTest`                                         | Runs one test class                                                                                                       |
| `./mvnw test -Dtest='RetroflowBusinessRulesTest#aTeam_cannotHaveTwoOpenRetrospectives'` | Runs one test method                                                                                                      |
| `./mvnw verify`                                                                         | Runs the tests, generates the coverage report, and **fails if line coverage is below 80%**. This is exactly what CI runs. |
| `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`                          | Mutation testing (PIT) of the `service` package with the service tests and vendor test. Takes a few minutes; not in CI.  |

The coverage report is written to `target/site/jacoco/index.html`.
On macOS, open it with `open target/site/jacoco/index.html`.
The mutation report is written to `target/pit-reports/index.html`.

| Test class                         | Covers                                                                                                                           |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `RetroflowBusinessRulesTest`       | The three core [business rules](#business-rules), written by the previous vendor. **Don't modify, skip, or delete these tests.** |
| `service/RetrospectiveServiceTest` | Service-layer details: happy paths, the exact reason each rule is rejected, input validation, unknown ids                         |

Tests use `@SpringBootTest` with their own in-memory H2 database, so no external services are
needed.

## Testing the API manually

### HTTP request file (recommended)

[`http/retroflow.http`](http/retroflow.http) contains ready-to-run requests:

- **IntelliJ IDEA:** open the file and click ▶ next to a request. The HTTP Client is built in.
- **VS Code:** install the **REST Client** extension (`humao.rest-client`) and click *Send Request*.

When you add an endpoint, add a request for it to this file.

### Postman collection

[`http/retroflow.postman_collection.json`](http/retroflow.postman_collection.json) has 33 requests, each one
with tests for the expected status code and error body:

- **Postman:** *Import* → choose the file. Run the folders top to bottom, or use the *Collection Runner*.
  Later requests reuse `teamId` and `retroId` from earlier ones, and every run creates a uniquely named
  team, so you can run it again without restarting the app.
- **Command line:** `npx newman run http/retroflow.postman_collection.json`.

Change `baseUrl` in the collection variables if the app doesn't run on `http://localhost:8080`.

### curl

```sh
curl -s http://localhost:8080/actuator/health
```

If you have [HTTPie](https://httpie.io/) or `jq`, they make JSON responses easier to read:

```sh
http :8080/actuator/health
curl -s http://localhost:8080/actuator/health | jq
```

### Inspecting the database (H2 console)

While the application is running, open http://localhost:8080/h2-console and log in with:

| Field     | Value                   |
|-----------|-------------------------|
| JDBC URL  | `jdbc:h2:mem:retroflow` |
| User name | `sa`                    |
| Password  | *(empty)*               |

The schema is generated by Hibernate from the JPA entities when the application starts.

## Configuration

All configuration is in `src/main/resources/application.properties`:

| Property                                  | Value                   | Notes                                                              |
|-------------------------------------------|-------------------------|--------------------------------------------------------------------|
| `spring.datasource.url`                   | `jdbc:h2:mem:retroflow` | In-memory. Data is lost on restart.                                |
| `spring.datasource.username` / `password` | `sa` / *(empty)*        | Read from `RETROFLOW_DB_USERNAME` / `RETROFLOW_DB_PASSWORD`; the values shown are the local fallbacks. Never write a real password in the file (standards §5.1). |
| `spring.jpa.hibernate.ddl-auto`           | `update`                | Hibernate creates the schema from the entities                     |
| `spring.h2.console.enabled`               | `true`                  | Development convenience. **Disable it outside local development.** |
| Actuator                                  | `/actuator/health` only | Spring Boot's default exposure                                     |

Override any property for one run without editing files, using environment variables or arguments:

```sh
SERVER_PORT=8081 ./mvnw spring-boot:run
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.show-sql=true
```

## Running with Docker

The multi-stage `Dockerfile` builds the application and runs it on a JRE 21 Alpine image, as a
non-root user, with a health check.

```sh
docker build -t retroflow .
docker run --rm -p 8080:8080 retroflow
```

With Podman, add `--format docker` to `podman build`, otherwise the `HEALTHCHECK` is ignored.


## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw verify` on every push and on pull
requests to `main`:

- The build fails if any test fails or if line coverage drops below 80%.
- Pull requests from branches in this repository get a comment with the coverage percentage.
- The JaCoCo HTML report is uploaded as the `jacoco-report` artifact of each run.

Dependabot (`.github/dependabot.yml`) opens weekly PRs for Maven dependencies and GitHub Actions.
The actions are pinned to commit SHAs, so let Dependabot update them rather than editing the
SHAs by hand.

## Project structure

```
.
├── .devcontainer/                  # Dev container: Java 21 + Maven
├── .github/
│   ├── dependabot.yml              # Dependency update PRs
│   └── workflows/ci.yml            # Build, test, coverage gate, PR comment
├── .mvn/wrapper/                   # Maven wrapper config (pins the Maven version)
├── http/retroflow.http             # Manual API requests
├── src/
│   ├── main/
│   │   ├── java/com/stackcraft/retroflow/
│   │   │   ├── RetroflowApplication.java   # Spring Boot entry point
│   │   │   ├── controller/                 # REST controllers, DTOs, global exception handler
│   │   │   ├── entity/                     # Team, Retrospective, FeedbackItem ← ActionItem, enums
│   │   │   ├── exception/                  # RetroflowException (rule violations), ResourceNotFoundException
│   │   │   ├── repository/                 # Spring Data JPA repositories
│   │   │   └── service/                    # TeamService, RetrospectiveService (business rules)
│   │   └── resources/application.properties
│   └── test/java/com/stackcraft/retroflow/
│       ├── RetroflowBusinessRulesTest.java # Vendor's business-rule tests (don't modify)
│       └── service/RetrospectiveServiceTest.java
├── Dockerfile
├── mvnw / mvnw.cmd                 # Maven wrapper scripts
└── pom.xml
```

Layers: `controller` (HTTP and DTOs) → `service` (business rules, transactions) →
`repository` (persistence). Controllers should not call repositories directly.

**Data model.** `ActionItem` extends `FeedbackItem` using JPA `SINGLE_TABLE` inheritance: all
items are stored in `feedback_item`, with an `item_kind` discriminator column. The
`priority` and `completed` columns are only filled in for action items.

## Business rules

The rules, and every interpretation the brief left open, are in [`decisions.md`](decisions.md).
[`test-matrix.md`](test-matrix.md) lists every rule × operation with its expected result, HTTP status,
and the test that covers it. All rules are enforced in the service layer.

## API endpoints

Errors use the format `{"error": "<Entity> <id>: <reason>", "status": 409, "timestamp": "…"}`.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/teams` | `{"name", "members": [..]}` | 201 + `Location` | 400, 409 name taken |
| GET | `/api/teams/{id}` | | 200 | 400, 404 |
| POST | `/api/teams/{teamId}/retrospectives` | `{"title", "date": "yyyy-MM-dd", "createdBy"}` | 201 + `Location` | 400, 403 not a member, 404, 409 one already OPEN |
| GET | `/api/teams/{teamId}/retrospectives` | | 200, newest first | 400, 404 |
| GET | `/api/retrospectives/{id}` | | 200 | 400, 404 |
| PUT | `/api/retrospectives/{id}/close` | `{"closedBy"}` | 200 | 400, 403, 404, 409 already CLOSED |

## Current status

| Area | Status |
|---|---|
| Entities, repositories, service layer with all business rules | ✅ Done |
| Team and retrospective endpoints, global exception handler | ✅ Done |
| Build, Docker image, CI, coverage gate | ✅ Done |
| Feedback endpoints (add, list, edit, delete) and action-item endpoints (PATCH, filter) | ❌ Next; see `test-matrix.md` |
| Changing an action item's priority | ❓ Not decided (`decisions.md`, O1) |

## Troubleshooting

| Symptom                                                                       | Cause / fix                                                                                                                     |
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `./mvnw: Permission denied`                                                   | `chmod +x mvnw`                                                                                                                 |
| `Web server failed to start. Port 8080 was already in use.`                   | Find the process with `lsof -i :8080`, or run on another port: `SERVER_PORT=8081 ./mvnw spring-boot:run` |
| `release version 21 not supported`                                            | Maven is using an older JDK. Check `./mvnw -v` and point `JAVA_HOME` to a JDK 21.                                               |
| My data disappeared                                                           | The database is in-memory and is reset on every restart, including DevTools restarts.                                           |

## Contact

The previous team lead has left the company, so there is no forwarding contact. Ping
#retroflow-support on Slack if you get stuck, though response times have been slow lately.

