# RetroFlow: audit against the StackCraft coding standards

**Standard:** *StackCraft Engineering — Coding Standards*, v3.1 (effective Q1 2025, last reviewed March 2025),
read live through the MCP server (`stackcraft-coding`). Section numbers and quotes are from that document.
**Code:** branch `block2`, everything under `src/`, plus `pom.xml`, `application.properties` and `README.md`.
**Sorting:** by the severity the standard gives (Critical → Major → Minor), then by section.
**Status values:** ✅ fixed · ⛔ blocked (needs a decision, see the finding) · ⏳ open (not Critical, not in scope for step 3).

## Summary

| # | Severity | § | Finding | Status |
|---|---|---|---|---|
| C1 | Critical | 1.4 | Field injection in 14 test classes (33 fields), including the vendor test | ⛔ needs test changes |
| C2 | Critical | 2.1 | `RetroflowException(String message)` lets anyone throw a free-form message | ✅ fixed |
| C3 | Critical | 2.2 | 404 unknown route, 405, 500 and `/error` return `"error": "Not Found"` etc., not the required format | ⛔ existing tests assert the current text |
| C4 | Critical | 4.1 | Non-member actor gets **403**, which is not in the status table (decision D3) | ⛔ conflicts with decision D3 |
| C5 | Critical | 5.1 | Datasource credentials written in `application.properties` | ✅ fixed |
| M1 | Major | 1.1 | Layer-first packages, not feature-first | ⏳ grandfathered (P6), confirm |
| M2 | Major | 1.3 | Concurrency test method is 33 lines | ⏳ |
| M3 | Major | 5.2 | Some input reaches the service unvalidated (blank PATCH title, empty PATCH body) | ⏳ |
| m1 | Minor | 1.2 | Four classes over 200 lines | ⏳ |
| m2 | Minor | 3.2 | Test names that don't start with the method name | ⏳ |
| m3 | Minor | 3.3 | Tests without Arrange/Act/Assert comments | ⏳ |
| m4 | Minor | 3.4 | Magic numbers in tests | ⏳ |
| m5 | Minor | 6.1 | JavaDoc names the wrong exception for a blank title | ⏳ |
| m6 | Minor | 6.2 | README endpoint list and status are out of date | ⏳ |
| U1 | none given | 3.1 | Branch coverage (70%) is not enforced, only line coverage | ⏳ |

Checked and compliant: §1.4 in production code, §2.3, §4.2, §4.3 and §5.3. See [Checked, no finding](#checked-no-finding).

---

## Critical

### C1. Field injection in the tests (§1.4) ⛔

**Where:** 33 `@Autowired` fields in 14 test classes, for example:
- `src/test/java/com/stackcraft/retroflow/RetroflowBusinessRulesTest.java:30` and `:33` (the **vendor** test)
- `src/test/java/com/stackcraft/retroflow/service/RetrospectiveServiceTest.java:39`, `:42`
- `src/test/java/com/stackcraft/retroflow/controller/TeamControllerTest.java:29`
- Full list: `grep -rn "@Autowired" src/test`.

Production code is clean, and `ArchitectureTest` enforces that. But `ArchitectureTest.java:26–28` deliberately leaves the tests out:
*"Production code only: tests may use field injection"* (`ImportOption.DoNotIncludeTests`).

**Standard, §1.4 Constructor injection only:** *"Field injection (`@Autowired` on a field) is prohibited. Constructor injection must be used for all Spring-managed dependencies."* Severity **Critical**.

**Why it matters:** the rule has no exception for tests. The reason the standard gives (*"makes testing impossible and hides dependencies"*) applies less to test classes. But the rule's wording does cover them, and a reviewer applying it literally will flag them.

**Correct implementation:**
```java
@SpringBootTest
class TeamServiceTest {

    private final TeamService teamService;

    @Autowired // on the constructor, not on a field
    TeamServiceTest(TeamService teamService) {
        this.teamService = teamService;
    }
}
```
Alternatively, set `spring.test.constructor.autowire.mode=all` in `src/test/resources/junit-platform.properties` so the constructor needs no annotation. In both cases, extend `ArchitectureTest` to cover test classes.

**Status: ⛔ blocked.** The fix means editing all 14 existing test classes. That includes `RetroflowBusinessRulesTest`, which may not be modified (vendor test, house rules). Step 3 also says existing tests stay untouched. **Decision needed:** may we rewrite our own 13 test classes? And does the coach accept the vendor test as a known exception?

---

### C2. Free-form exception constructor (§2.1) ✅

**Where:** `src/main/java/com/stackcraft/retroflow/exception/RetroflowException.java:13–15`
```java
public RetroflowException(String message) {
    super(message);
}
```

**Standard, §2.1 Exception message format:** *"All exceptions thrown by the service layer must follow this exact format: `[EntityType] [identifier]: [human-readable reason]`"*. Severity **Critical**.

**Why it matters:** every current `throw` uses the three-argument constructor, so every message today is in the format. But this public constructor lets the next developer write `throw new RetroflowException("oops")`. That compiles, gives a 409 and **breaks the alerting**, which §2.1 says *"parses exception messages for alerting and dashboards"*. Nobody calls the constructor (`grep` finds no caller in `src/`), so it's pure risk.

**Correct implementation:** remove the constructor, so the only way to build the exception is `RetroflowException(entityType, identifier, reason)`, which always formats. A test guards the format of every domain exception and checks that no free-form constructor comes back.

**Fix:** removed the constructor. Added `src/test/java/com/stackcraft/retroflow/exception/ExceptionMessageFormatTest.java`, which:
- checks that each domain exception produces `<EntityType> <identifier>: <reason>`, with `new` when there's no id yet;
- checks by reflection that no domain exception has a public `(String)` constructor.

Before the fix, the reflection test failed (proof). After the fix, `./mvnw test` is green.

---

### C3. Errors outside the required body format (§2.2) ⛔

**Where:**
- `src/main/java/com/stackcraft/retroflow/controller/GlobalExceptionHandler.java:113`: Spring's own errors return only the reason phrase (`"Not Found"`, `"Method Not Allowed"`).
- `src/main/java/com/stackcraft/retroflow/controller/GlobalExceptionHandler.java:116`: unexpected errors return `"Unexpected error"`.
- `src/main/java/com/stackcraft/retroflow/controller/ApiErrorController.java:23`: `/error` returns only the reason phrase.

**Standard, §2.2 Global exception handler required:** *"Required error response format: `{ "error": "EntityType identifier: reason", "status": 400, "timestamp": "…" }`"*. Severity **Critical**.

**Why it matters:** the body has the right *fields*, but the `error` value isn't `EntityType identifier: reason`. The monitoring that parses these values (§2.1) can't classify exactly the most important errors: 500s and unknown routes.

**Correct implementation:** use the pseudo-entity the team already uses for request-level errors (P10, P11):
```java
return respond(status, INVALID_REQUEST + reasonPhrase);          // "Request new: Not Found"
return respond(HttpStatus.INTERNAL_SERVER_ERROR, INVALID_REQUEST + "unexpected error"); // 500
// ApiErrorController
ApiError.of("Request new: " + status.getReasonPhrase(), status.value());
```

**Status: ⛔ blocked.** These existing tests assert the current text exactly:
- `ErrorFormatContractTest.java:129` (`"Not Found"`)
- `ErrorFormatContractTest.java:169` (`"Unexpected error"`)
- `TeamControllerTest.java:124` (`"Not Found"`)

The fix can't pass without changing them, and step 3 forbids that. **Decision needed:** approve updating those three assertions. Related open point: is `Request new:` itself acceptable as "EntityType identifier"? It's a team interpretation (P10, P11), not something the standard spells out.

---

### C4. 403 is not in the mandatory status table (§4.1) ⛔

**Where:** `src/main/java/com/stackcraft/retroflow/controller/GlobalExceptionHandler.java:41–44` maps `NotTeamMemberException` → `HttpStatus.FORBIDDEN`. Thrown from `service/ServiceInput.java:34`.

**Standard, §4.1 HTTP status codes:** *"The following status codes are mandatory for these situations: … Business rule violation | 409 Conflict … Using 200 for a creation, or 400 for a business rule violation, is a Critical violation."* Severity **Critical**. The table has no row for 403.

**Why it matters:** "only a team member may create or close a retrospective" is a business rule from the brief. Under the standard that means 409. `decisions.md` D3 calls 403 *"a deliberate exception to the standards' status table"*. A team decision doesn't override a Critical company standard by itself, and API consumers following the standard won't expect 403.

**Correct implementation (if the standard wins):** delete the `notTeamMember` handler. `NotTeamMemberException extends RetroflowException`, so the existing `businessRuleViolation` handler already maps it to 409. Then update D3 and the HTTP status policy in `decisions.md`.

**Status: ⛔ blocked.** This is a conflict between `decisions.md` (which "wins" per the house rules) and the standard. Existing tests assert 403, for example `ErrorFormatContractTest` `notAMember_returns403InTheErrorFormat` and `RetrospectiveControllerTest` `createRetrospective_byNonMember_returns403`. **Decision needed** from the team and coach (or the StackCraft Engineering Guild, the standard's contact): keep 403 as a documented exception, or switch to 409.

---

### C5. Credentials in `application.properties` (§5.1) ✅

**Where:** `src/main/resources/application.properties:7–8`
```properties
spring.datasource.username=sa
spring.datasource.password=
```

**Standard, §5.1 No secrets in code or configuration files:** *"API keys, passwords, and tokens must never appear in: … `application.properties` or `application.yml` … Use environment variables or a secrets manager."* Severity **Critical**.

**Why it matters:** the values are H2's harmless in-memory defaults today. But the password property sits in the file, so the moment someone points this at a real database, the obvious edit is to type the real password there, into Git history.

**Correct implementation:** read both from the environment, keeping the H2 defaults only as a fallback for local runs:
```properties
spring.datasource.username=${RETROFLOW_DB_USERNAME:sa}
spring.datasource.password=${RETROFLOW_DB_PASSWORD:}
```

**Fix:** applied as above, and the README configuration table now names the environment variables. Added `src/test/java/com/stackcraft/retroflow/ConfigurationSecretsTest.java`. It fails if any `password`, `secret`, `token` or `api-key` property in `application.properties` has a literal value instead of a `${…}` placeholder. Before the fix it failed (proof). After the fix, `./mvnw test` is green.

---

## Major

### M1. Layer-first package structure (§1.1) ⏳

**Where:** `src/main/java/com/stackcraft/retroflow/{controller,service,repository,entity,exception}`.

**Standard, §1.1 Package structure:** *"All projects must follow a feature-first package structure, not a layer-first structure. … Existing projects are grandfathered but new projects must comply."* Severity **Major**.

**Why it matters, and why it's tricky:** the code is exactly the "Incorrect" example in the standard. Whether that's a violation depends on one word: is RetroFlow an *existing* project (inherited from the vendor, layer-first on purpose) or a *new* one (StackCraft's own rebuild)? `decisions.md` P6 says "existing, grandfathered", but P6 is an AI-proposed default the team hasn't confirmed. A big-bang move would touch every file and every import, including `ArchitectureTest`'s layer rules, for no behaviour change.

**Correct implementation, if it's ruled "new":** `com.stackcraft.retroflow.team` (`TeamController`, `TeamService`, `TeamRepository`, `Team`, DTOs), `…retrospective`, `…feedback` (feedback and action items), and `…common` (`GlobalExceptionHandler`, `ApiError`, exceptions). Move one feature per commit with the tests green, and rewrite the `ArchitectureTest` layer rules as per-feature rules.

**Recommendation:** confirm P6 with the coach. Don't refactor without that.

### M2. Method longer than 20 lines (§1.3) ⏳

**Where:** `src/test/java/com/stackcraft/retroflow/service/RetrospectiveServiceTest.java:75` `createRetrospective_concurrentRequests_opensExactlyOne`: 33 lines without blanks and comments. Production code is within the limit (`RetrospectiveService.updateRetrospective`, `:200`, is exactly 20).

**Standard, §1.3 Method length:** *"No method may exceed 20 lines (excluding blank lines and comments). Methods that exceed this must be broken into private helper methods with descriptive names."* Severity **Major**.

**Why it matters:** a concurrency test is hard to read anyway, and the length hides the three steps.

**Correct implementation:** extract `submitConcurrentCreates(int requests, CountDownLatch start)` and `countOutcomes(List<Future<Retrospective>>)` (returning a small `record Outcome(int created, int rejected)`). The test then has only Arrange, Act and Assert.

### M3. Input not validated at the boundary (§5.2) ⏳

**Where:**
- `src/main/java/com/stackcraft/retroflow/controller/dto/RetrospectiveDtos.java:36`: `title` has `@Size` but no not-blank check, so `"title": "  "` reaches the service.
- `src/main/java/com/stackcraft/retroflow/controller/dto/RetrospectiveDtos.java:35–40` and `controller/dto/ActionItemDtos.java:18`: "at least one field" is checked only in the service (`RetrospectiveService.java:202`, `FeedbackService.java:189`).

**Standard, §5.2 Validate all input at the boundary:** *"All data entering the system via API must be validated before it reaches the service layer. The controller is the boundary. Do not assume the service will catch bad data."* Severity **Major**.

**Why it matters:** the service does catch these cases today (400), so this isn't a bug. But the standard explicitly forbids relying on that.

**Correct implementation:**
```java
public record UpdateRetrospectiveRequest(
        @Size(max = FieldLimits.MAX_TITLE_LENGTH) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
        LocalDate date, RetrospectiveStatus status,
        @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String updatedBy) {

    @AssertTrue(message = "send title, date and/or status")
    boolean isAnythingToUpdate() { return title != null || date != null || status != null; }
}
```
Do the same for `UpdateActionItemRequest`. Keep the service checks as a second line of defence.

---

## Minor

### m1. Classes over 200 lines (§1.2) ⏳

**Where:**
- `service/RetrospectiveService.java` (250 lines)
- `service/FeedbackService.java` (246)
- `src/test/…/service/RetrospectiveServiceTest.java` (238)
- `src/test/…/controller/RetrospectiveControllerTest.java` (223)

**Standard, §1.2 Class length:** *"No class may exceed 200 lines. If a class approaches this limit, it is doing too much."* Severity **Minor**.

**Why it matters:** `RetrospectiveService` mixes the retro lifecycle with vendor-test pass-throughs. `FeedbackService` mixes feedback and action items.

**Correct implementation:**
- Split `FeedbackService` into `FeedbackService` and `ActionItemService` (complete, uncomplete, changePriority, update, list).
- The vendor pass-throughs in `RetrospectiveService` must stay there, because the vendor test calls them on that class. Shrink them to one-line delegates with shorter JavaDoc that links to the target.
- Split the large test classes per operation.

### m2. Test naming (§3.2) ⏳

**Where:**
- `RetroflowBusinessRulesTest.java:37`, `:52`, `:64` (vendor test, can't be renamed)
- `ErrorFormatContractTest` (e.g. `malformedJson_returns400InTheErrorFormat`, `successResponses_areNotErrors`)
- `ResponseContractTest` (`teamResponse_hasIdNameAndMembers`)
- `FieldLimitsTest` (`fieldLimits_matchTheEntityColumnSizes`)

**Standard, §3.2 Test naming convention:** *"Test method names must follow: `methodName_condition_expectedBehaviour`"*. Severity **Minor** (*"but enforced in code reviews"*).

**Why it matters:** these tests cover cross-cutting behaviour, not one method, so they start with a topic instead. Reviewers will still flag them.

**Correct implementation:** name them after the handler or mapper method under test, e.g. `unreadableBody_withMalformedJson_returns400InTheErrorFormat` and `toResponse_withTeam_hasIdNameAndMembers`. Note: the house rules also say tests are "named after the rule" in `test-matrix.md`, which is a second convention to reconcile.

### m3. Arrange-Act-Assert comments missing (§3.3) ⏳

**Where:** `ErrorFormatContractTest` (15 tests), `ResponseContractTest` (3) and `FieldLimitsTest` (1) have no `// Arrange`, `// Act` or `// Assert` comments at all. Other classes use them.

**Standard, §3.3 Arrange-Act-Assert with comments:** *"All tests must use the AAA pattern with explicit comments"*. Severity **Minor**.

**Why it matters:** consistency. A reviewer can't see at a glance what is being tested.

**Correct implementation:** add `// Arrange` (where there's setup) and `// Act & Assert` to each test. This is a comment-only change, but it still touches existing tests, so it needs approval like C1.

### m4. Magic numbers in tests (§3.4) ⏳

**Where:**
- `RetrospectiveServiceTest.java:97` (`result.get(10, TimeUnit.SECONDS)`)
- `ErrorFormatContractTest` (literal ids such as `/api/action-items/1`, `getTeam(1L)`)

**Standard, §3.4 No magic numbers in tests:** *"Test data must use named constants or clearly named local variables."* Severity **Minor**.

**Why it matters:** `10` seconds and id `1` look meaningful but aren't explained.

**Correct implementation:** `private static final Duration MAX_WAIT_PER_REQUEST = Duration.ofSeconds(10);` and `private static final long ANY_ID = 1L;`.

### m5. JavaDoc names the wrong exception (§6.1) ⏳

**Where:** `src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java:69`. The vendor overload documents `@throws RetroflowException if the title is blank`, but a blank title throws `InvalidInputException` (400, not 409).

**Standard, §6.1 JavaDoc on all public service methods:** *"`@throws` for each exception that can be thrown"*. Severity **Minor**.

**Why it matters:** a reader expects a 409 where the API gives a 400.

**Correct implementation:** `@throws InvalidInputException if the title is blank` on its own line, and keep `RetroflowException` for "already has an OPEN retrospective". All other public service methods are complete.

### m6. README out of date (§6.2) ⏳

**Where:**
- `README.md:286–293`: the endpoint list misses `PATCH /api/retrospectives/{id}`, `GET /api/retrospectives/{id}/action-items` and `PATCH /api/action-items/{id}`.
- `README.md:302–303`: still says action-item endpoints are "Next" and priority changes are "Not decided" (decided as D14).

**Standard, §6.2 README requirements:** *"A link to the API documentation or a brief endpoint list"*. Severity **Minor**.

**Why it matters:** a new developer trusts the README.

**Correct implementation:** add the three rows (status codes from the controller JavaDoc), and update the status table.

---

## No severity given in the standard

### U1. Branch coverage not enforced (§3.1) ⏳

**Where:** `pom.xml:123–131`. The JaCoCo `check` rule has only a `LINE` limit.

**Standard, §3.1 Minimum coverage:** *"Line coverage must be at least 80%. Branch coverage must be at least 70%. The CI pipeline enforces this — builds will fail below these thresholds."* No severity is given.

**Why it matters:** the claim "CI enforces this" is only half true here. Current branch coverage is 94% (last `target/site/jacoco/jacoco.csv`), so adding the gate costs nothing now and protects later.

**Correct implementation:** add a second `<limit>` to the existing rule:
```xml
<limit>
    <counter>BRANCH</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.70</minimum>
</limit>
```

---

## Checked, no finding

| § | What was checked |
|---|---|
| 1.4 (production) | Every Spring bean uses constructor injection. `ArchitectureTest.noFieldInjection` enforces it. |
| 2.1 (messages) | Every `throw` in `service/` goes through `RetroflowException(entityType, identifier, reason)` or `ResourceNotFoundException(entityType, id)`. Before an entity has an id, the identifier is `new` (a team interpretation in `decisions.md`; the standard's examples always show an id). |
| 2.3 | The only `catch` (`RetrospectiveServiceTest.java:99`) asserts on the exception and counts it. `GlobalExceptionHandler` logs and translates, and doesn't swallow. |
| 4.2 | No stack traces, class names or SQL in responses. Database races are logged and answered with a generic 409 (`GlobalExceptionHandler.java:82–86`). |
| 4.3 | All list endpoints return a plain JSON array. |
| 5.3 | The only `@Query` (`ActionItemRepository.java:17–25`) uses named parameters. No string concatenation. |

**Outside the standard (not a finding):** the H2 web console is enabled (`application.properties:14`). The README says to disable it outside local development, but nothing enforces that.

---

## Self-check (done by the AI, so still spot-check it yourself)

- **Quotes:** the quotes for §1.4, §2.1 and §5.1 were compared with the document text returned by the MCP server. All three are in it word for word.
- **Lines:** `RetroflowException.java:13` and `application.properties:7–8` show exactly the quoted code (before the fixes). The first draft had **three wrong line numbers**: vendor test `:51`/`:63` instead of `:52`/`:64`, and `RetrospectiveServiceTest:107` instead of `:97`. They were found with `sed -n` and corrected. Good material for Retro 3: the AI counts lines badly from memory.
- **§1.1:** found, see M1. It's tricky because the standard grandfathers existing projects, and the starter was layer-first on purpose. Whether RetroFlow counts as "existing" is a judgment call, not a fact in the code.

---

## Fix log (step 3: Critical findings)

One finding per step. Each new test was run **before** the fix to prove it fails, then the full suite ran after the fix. No existing test file was changed (`git status` shows only new test files).

| Step | Finding | What was wrong | Based on | Change | New test | Result |
|---|---|---|---|---|---|---|
| 1 | C2 | A public `RetroflowException(String)` constructor allowed messages outside the format | §2.1 | Constructor removed (`RetroflowException.java`) | `exception/ExceptionMessageFormatTest` (3 tests). Before the fix: 1 failure, "a (String message) constructor bypasses the §2.1 format" | `./mvnw test`: 137 tests, 0 failures |
| 2 | C5 | The datasource password (and user) were literal values in `application.properties` | §5.1 | `${RETROFLOW_DB_USERNAME:sa}` / `${RETROFLOW_DB_PASSWORD:}`, README config table updated | `ConfigurationSecretsTest` (1 test). Before the fix: failure on `spring.datasource.password` | `./mvnw verify`: 138 tests, 0 failures, coverage gate met |
| – | C1 | Field injection in tests | §1.4 | **Not fixed:** would change 14 existing test classes, one of them the vendor test | – | Decision needed |
| – | C3 | `"Not Found"` / `"Unexpected error"` not in the error format | §2.2 | **Not fixed:** 3 existing assertions expect the current text | – | Decision needed |
| – | C4 | 403 for non-members | §4.1 | **Not fixed:** conflicts with decision D3 and existing tests | – | Decision needed |
