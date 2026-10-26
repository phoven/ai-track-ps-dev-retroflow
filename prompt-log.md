# RetroFlow: prompt log

Our team's prompt library. Every prompt we send goes in here, followed by a one-line verdict:
did it work as-is, or did we have to fix something, and what did we change?
We reuse the good ones in Block 3.

**Format:** newest entry at the bottom. Verdict values: ✅ worked as-is · 🔧 worked after a fix · ❌ didn't work.

---

## 1. Open a pull request to a fork

```
https://github.com/nielsvanzandbergen-cegeka/ai-track-nielsvz.git create a pullrequest to my fork
```

**Verdict:** ❌ Too vague: the prompt didn't say which branch, which base, or what the PR is about. We cancelled the git/`gh` inspection step. Next time: name the source branch, the target repo and base branch, and give a title and summary.

---

## 2. Write the house rules file

```
crate .github/copilot-instructions.md based on prompts in /Users/phoven/Code/cegeka/ai-track-ps-dev/block1.md
```

**Verdict:** 🔧 Worked. The AI also read `pom.xml`, `application.properties` and `decisions.md` so the rules match the real stack (Maven/H2, not Gradle/PostgreSQL) and our status code table. We still have to edit the wording into our own words and commit it.

---

## 3. Start this prompt log

```
Keep a prompt log in prompt-log.md in the root of the repo. Every time you write a prompt,
paste it in and add one line: did it work, or did you have to fix it, and what did you change?
This is the start of your team's prompt library, and you'll use it again in Block 3.
```

**Verdict:** ✅ Worked. The AI filled in this file, including the earlier prompts from this session, and added a rule to `.github/copilot-instructions.md` so the AI keeps adding to this log.

---

## 4. Plan: vendor tests and enforcing the test matrix

```
RetroflowBusinessRulesTest, test-matrix.md, decisions.md, the service classes and the brief
Making the three tests in RetroflowBusinessRulesTest compile and pass: which service methods are missing, or have a different signature than the test calls
Enforcing every rule from test-matrix.md in the service layer, and which of those rows already have a test
Where each rule gets enforced, and which exception it throws
```

**Verdict:** ✅ Worked as-is. Read-only plan, not run (`./mvnw test` still to do). No vendor methods are missing. Found: the exception hierarchy lets vendor tests pass for the wrong reason, row 1.4 and B.10 are untested, and O1 and row 2.3 still need decisions.

---

## 5. Save the plan

```
Save the resulting plan as is into a file (`plan-${camelCaseName}.prompt.md` without frontmatter) for further refinement.
```

**Verdict:** ✅ Worked. Saved as `plan-vendorTestsAndMatrixEnforcement.prompt.md` in the repo root.

---

## 7. Execute the plan

```
execute the plan
```

**Verdict:** 🔧 Worked after fixes. The plan was partly wrong: the exact-exception tests (step 2) and team service tests (step 5) already existed, so they weren't added again. Added: B.10 filter endpoint with service and controller tests, and the row 1.4 concurrency test (checked to fail without the team lock). Fixed: parallel edits to the same file overwrote each other and had to be redone one at a time. Open decisions (O1, row 2.3, setters) were not made. `./mvnw verify`: 76 tests pass, coverage gate met.

---

## 8. Decide O1: action item priority can change

```
action items prio acn change
```

**Verdict:** 🔧 Worked; terse prompt, the AI filled in the details from D1 and D8. Recorded as D14 (priority can change while the retro is OPEN, 409 after close; also for completed items). Built `PATCH /api/action-items/{id}` with `priority` and/or `completed` in one transaction. Matrix rows 3.8/3.9 now tested; 4.1–4.4 got endpoint tests. `./mvnw verify`: 90 tests pass, coverage gate met. Check the "also for completed items" and "empty body = 400" choices.

---

## 9. Retro update endpoint, no public setters, explain O2

```
yes to update endopint, no public setters, and what is 02 actually?
```

**Verdict:** 🔧 Worked; terse prompt, the AI filled in the endpoint details. Recorded as D15: `PATCH /api/retrospectives/{id}` (title/date, OPEN only, member actor, `status: OPEN` on a closed retro = 409 reopen). D12 changed to "no public setters": they were replaced by `rename`, `reschedule`, `editContent` and `changePriority`, and `Team`'s and `setAuthor` were removed. Matrix rows 2.3–2.5 and B.12 are now tested; no rows are untested any more. O2 explained, left open for the team and coach. `./mvnw verify`: 104 tests pass, coverage gate met.

---

## 10. Mutation testing with PIT

```
add a mutation testing tool for Maven (PIT) and to check that the plugin version it picks really exists before it uses it
Limits the run to the service classes and your test classes
Wants the surviving mutants listed: file, line, and what was changed
Wants new tests that kill them, without changing any production code
Asks for the mutation score before and after
```

**Verdict:** ✅ Worked. Checked the versions on Maven Central before using them (pitest-maven 1.30.1, pitest-junit5-plugin 1.2.3, both the latest release, HTTP 200, no CVEs). Scope: `service.*` classes, `service.*Test` and the vendor test. Score went from 86/89 (97%: 1 survived, 2 not covered) to 89/89 (100%) with 5 new tests and no production changes. Small fix: one long terminal command was garbled by line-wrapping, so I split it into shorter ones. `./mvnw verify`: 109 tests pass.

---

## 11. Pair BD step 1: plan before you act (web layer)

```
Step 1: plan before you act
Read: the controllers, the brief and decisions.md
Create: nothing in the code. Paste the plan in prompt-log.md. The status code you decide on goes in decisions.md
Goal: a plan, and no changed files yet, for three things:

Which endpoints exist, and what each of them returns today (status code and body), compared to the brief and decisions.md
Contract tests for every endpoint: success, invalid input (400), not found (404) and a business rule violation
A restructuring: DTOs instead of entities in requests and responses, one global exception handler with one error format, validation on the request objects and no logic in the controllers
A good prompt for this:

Tells the agent which files to read first
Says in so many words that nothing may be changed yet
Asks for a plan per part, and for the file and class behind every planned change
Says what to do when the brief doesn't give an answer (hint: flag it, don't decide it)
```

**Verdict:** 🔧 Worked after interpretation. This pasted the exercise text, not a prompt, and an earlier "interpret this" prompt with no attachment was cancelled. The AI read it as the well-formed prompt: read the controllers, brief and decisions.md first, change no code, give a plan per part with file and class, flag gaps. Business-rule status is **409**, already in `decisions.md`. Gaps were added there as O3–O6, not decided. No code changed.

### Plan

**Read first:** `controller/TeamController`, `RetrospectiveController`, `ActionItemController`, `GlobalExceptionHandler`, `ApiError`, `controller/dto/*Dtos`, the brief, `decisions.md`.
**Off limits for pair BD:** `service/`, `entity/`, `repository/` (pair AC). Where the plan needs something there, it's flagged as a request to AC.

#### Part 1: endpoints today vs the brief and decisions.md

All errors already use one body: `{"error": "<Entity> <id>: <reason>", "status": n, "timestamp": "…"}` (`ApiError`).

| # | Endpoint | Success (body) | Errors today | vs brief / decisions |
|---|---|---|---|---|
| 1 | `POST /api/teams` `{name, members[]}` | 201 + `Location`, `TeamResponse{id, name, members}` | 400 blank name/no members ("Request new: …"), 400 duplicate member (D10), 409 name taken (D10) | ✅ |
| 2 | `GET /api/teams/{id}` | 200 `TeamResponse` | 400 non-numeric id, 404 | ✅ |
| 3 | `POST /api/teams/{teamId}/retrospectives` `{title, date, createdBy}` | 201 + `Location`, `RetrospectiveResponse{id, teamId, title, date, status}` | 400, 403 non-member (D3), 404 team, 409 already one OPEN | ✅ |
| 4 | `GET /api/teams/{teamId}/retrospectives` | 200 `[RetrospectiveResponse]`, newest first (P2) | 400, 404 | ✅ |
| 5 | `GET /api/retrospectives/{id}` | 200 `RetrospectiveResponse` | 400, 404 | ✅ (not in the brief, harmless) |
| 6 | `PUT /api/retrospectives/{id}/close` `{closedBy}` | 200 `RetrospectiveResponse` (CLOSED) | 400, 403, 404, 409 already CLOSED (D7) | ✅ |
| 7 | `PATCH /api/retrospectives/{id}` `{title?, date?, status?, updatedBy}` | 200 `RetrospectiveResponse` | 400, 403, 404, 409 CLOSED/reopen (D15) | ✅ |
| 8 | `GET /api/retrospectives/{id}/action-items?priority=&completed=` | 200 `[ActionItemResponse{id, retrospectiveId, content, author, priority, completed}]` | 400 bad filter, 404 | ✅ D9. No business-rule case exists for a read. Unknown params ignored ⚠ O6 |
| 9 | `PATCH /api/action-items/{id}` `{priority?, completed?}` | 200 `ActionItemResponse` | 400 empty/invalid body, 404, 409 (D7, D8, D14) | ✅. Unknown enum in body gives a generic 400 message ⚠ O5 |
| — | **Missing:** add feedback item | – | – | ❌ brief: "Feedback items can be added". ⚠ O3 (URL) |
| — | **Missing:** list feedback per retrospective | – | – | ❌ brief: "can be retrieved per retrospective". Needs a service method (AC). ⚠ O3, O4 |
| — | **Missing:** edit / delete feedback item | – | – | ❌ D5 (service methods exist, no endpoint). ⚠ O3 |

#### Part 2: contract tests (web layer only, services mocked)

- **Style:** `@WebMvcTest(XController.class)`, which includes `GlobalExceptionHandler` automatically, with `@MockBean` for the services. Spring Boot 3.3, so `@MockBean`; `@MockitoBean` only arrives in 3.4. JSON in, assertions with `jsonPath` on status **and** body (`$.error`, `$.status`, `$.timestamp`), never on Java classes. Errors come from the mock throwing the domain exception.
- **⚠ Test-data problem:** entities have no id setter (D12, ids are generated), so a mocked service can't easily return a `Team` with an id. Proposal: a test helper that sets `id` with `ReflectionTestUtils.setField`. Test code only; entities stay untouched.
- **New files** in `src/test/java/com/stackcraft/retroflow/controller/`. The existing `*ControllerTest` classes are full-stack and stay as they are.

| File / class | Endpoint | Success | 400 | 404 | Rule violation |
|---|---|---|---|---|---|
| `TeamControllerContractTest` | 1 | 201 + Location + body | blank name, empty members, malformed JSON | – | 409 name taken |
| | 2 | 200 body | `/api/teams/abc` | unknown id | – (none for a read) |
| `RetrospectiveControllerContractTest` | 3 | 201 + Location + body | missing date, bad date format | team unknown | 409 already OPEN, 403 non-member |
| | 4 | 200 list | bad id | team unknown | – |
| | 5 | 200 | bad id | unknown | – |
| | 6 | 200 CLOSED | blank `closedBy` | unknown | 409 already CLOSED, 403 |
| | 7 | 200 | empty body, unknown `status` value | unknown | 409 reopen, 409 closed-modify, 403 |
| | 8 | 200 filtered list | `priority=URGENT`, `completed=maybe` | unknown retro | – |
| `ActionItemControllerContractTest` | 9 | 200 body | `{}`, `"priority":"URGENT"` | unknown | 409 completed twice, 409 uncomplete, 409 priority in CLOSED retro |
| `FeedbackItemControllerContractTest` (after O3) | add / list / edit / delete | 201 / 200 / 200 / 204 | blank content, unknown type, priority on a non-action type (D6) | unknown retro / item | 409 in a CLOSED retro (add, edit, delete) |

- Tests are run and failures reported, not fixed (step 2). Expected failures: the feedback endpoints (don't exist, 404/405) and the O5 message if we decide it should be specific.

#### Part 3: restructuring (web layer only)

Already in place, nothing to do:
- **DTOs:** no controller accepts or returns an entity.
- **One error handler and format:** `GlobalExceptionHandler` + `ApiError`.
- **Validation** on `Create*Request` and `CloseRetrospectiveRequest`.
- **Thin controllers:** they only map and build `Location`.

Planned changes:

| # | Change | File / class | Depends on |
|---|---|---|---|
| R1 | Feedback endpoints: add, list, edit (PUT content), delete (204) | new `controller/FeedbackItemController`, new `controller/dto/FeedbackItemDtos` (`AddFeedbackItemRequest{content, type, author, priority?}` with `@NotBlank`/`@NotNull`/`@Size`, `UpdateFeedbackItemRequest{content}`, `FeedbackItemResponse`) | O3, O4. AC: a `FeedbackService.listFeedbackItems(retroId)` method |
| R2 | Meaningful 400 for an unknown enum in a JSON body: field name + allowed values, same wording as the query-parameter case | `GlobalExceptionHandler.unreadableBody` (inspect the `InvalidFormatException` cause) | O5 |
| R3 | `@Valid` on the PATCH body for consistency (no constraints yet, but it keeps the pattern) | `ActionItemController.updateActionItem` | – |
| R4 | Move "title not blank" to the request object (`@Pattern(regexp = ".*\\S.*")` on `title`), so it's a 400 at the web layer; the service check stays as a second line | `RetrospectiveDtos.UpdateRetrospectiveRequest` | – |
| R5 | Spring's own 400s (missing parameter, wrong media type) get the `"Request new: …"` message format instead of only the reason phrase | `GlobalExceptionHandler.unexpected` (or dedicated handlers) | – |

- **Not planned, flagged for step 4 (ArchUnit):** the controllers import `entity.Priority` and `entity.RetrospectiveStatus` (enums), and the DTO `from(entity)` mappers live in `controller.dto`. A rule "controllers don't depend on `entity`" would fail on these. Either allow enums in the rule, or move the enums (AC's package).
- **Order for step 3:** R3 → R4 → R5 → R2 → R1. Smallest first, and R1 last because it waits on O3, O4 and AC. Run the contract tests after each one.

---

## 12. Is there a UI?

```
is there already a ui i can test
```

**Verdict:** ✅ Answered from the repo: no frontend (out of scope in the brief) and no Swagger UI. Pointed to `http/retroflow.http`, the H2 console and actuator health. Offered springdoc as an option without adding it.

---

## 13. Postman collection

```
create a postman collection which i can use to test the api
```

**Verdict:** ✅ Worked. `http/retroflow.postman_collection.json` (v2.1) has 33 requests for every endpoint: success, 400, 403, 404 and 409, with tests for the status and the error body. It's re-runnable thanks to a unique team name per run. Generated by a script so the JSON is valid. Ran it against the live app with `npx newman` twice: 66 requests, 148 assertions, 0 failures. Feedback and action-item changes aren't covered: there's no endpoint to create them yet (O3).

---

## 14. Where's the collection?

```
wheres the collection
```

**Verdict:** ✅ Pointed to `http/retroflow.postman_collection.json` and opened it in the editor.

---

## 15. Default payloads

```
add default payloads to the collection
```

**Verdict:** 🔧 Ambiguous. Every POST/PUT/PATCH already had a request body, so the AI read it as "saved example responses". Captured from the running app (33 examples, every status as expected), not invented. `newman` re-run: 33 requests, 74 assertions, 0 failures. Fixed: the first script file wasn't written to disk and had to be created again. Next time say "example responses" or "request bodies".

---

## 16. Connection refused

```
connect ECONNREFUSED 127.0.0.1:8080
```

**Verdict:** 🔧 The AI's own fault: it stopped the app after capturing the examples. It restarted `./mvnw spring-boot:run` in the background and confirmed `/actuator/health` is UP. Lesson: say when you stop something the user may still need.

---

## 17. Plan: web layer, no entities in requests/responses, one error format

```
create changes in the web layer only.  Check for entities in the request and responses.  Plan to remove these.  Plan changes to hqve only one @RestControllerAdvice and one error format.
```

**Verdict:** ✅ Plan only (read-only mode). Found: no controller accepts or returns an entity, but the DTOs import entities for mapping and length limits, and they expose the `Priority`/`RetrospectiveStatus` enums. There's already one `@RestControllerAdvice`, but `/error`, Spring's own 400s and DB race errors (500) fall outside the format. Open: enums, 409 for races, actuator.

---

## 18. Save the plan

```
Save the resulting plan as is into a file (`plan-${camelCaseName}.prompt.md` without frontmatter) for further refinement.
```

**Verdict:** ✅ Saved as `plan-webLayerDtosAndSingleErrorFormat.prompt.md` in the repo root.

---

## 19. Execute the web-layer plan

```
enterprice engage
```

**Verdict:** 🔧 Worked after a fix. Read as "make it so": carry out plan 17/18. The plan's open choices were taken as recommended and recorded as P8–P12 in `decisions.md` (they can be reversed). Changes are in the web layer only:
- `ResponseMapper` added; DTOs no longer contain entity classes.
- `FieldLimits` added.
- `ApiErrorController` on `/error`.
- Enum-in-body message (resolves O5).
- 409 for database races.
- `@Valid` added on the action-item PATCH.

New tests: `ErrorFormatContractTest` (15), `ResponseContractTest` (3) and `FieldLimitsTest` (1). Existing tests are unchanged.

Fixed: the new mock-based tests failed because the local JDK is **Java 25** while the project and CI use 21. Mockito's Byte Buddy (Spring Boot 3.3.4) can't mock on 25. The project wasn't changed for this; the check ran with `JAVA_TOOL_OPTIONS=-Dnet.bytebuddy.experimental=true`, and `./mvnw verify` gave 128 tests, 0 failures, coverage met. Install a JDK 21 locally to match CI.

---

## 20. Fix the build

```
fix the build
```

**Verdict:** ✅ Reproduced first: a plain `./mvnw verify` on the local JDK 25 had 18 errors, all "Java 25 (69) is not supported by the current version of Byte Buddy" (1.14.19, managed by Spring Boot 3.3.4). Fix: `byte-buddy.version` = 1.18.16 in `pom.xml`, checked before use (latest release, both jars HTTP 200, no CVEs). It also works on Java 21 (CI). No experimental flag and no test changes. `./mvnw verify`: 128 tests, 0 failures, coverage met.

---

## 21. Pair BD step 4: architecture test (ArchUnit)

```
Step 4: lock it in with an architecture test
A restructuring that nobody enforces will be undone in two months. You can turn the rules into tests with ArchUnit, a library that checks the structure of your code.

Read: the web layer as it is after step 3, and pom.xml
Create: the ArchUnit dependency in pom.xml and one architecture test class in src/test
Goal: a test class with at least three architecture rules, for example: controllers don't use repositories, controllers don't return entities, services don't depend on controllers. Plus proof that the rules can actually fail.

A good prompt for this:

Asks the agent to add ArchUnit as a test dependency, and to check that the artifact and version really exist before adding them
Names the rules (or asks for the three most useful ones, with a reason)
Wants proof: break each rule on purpose, show that the test fails, then put the code back
```

**Verdict:** ✅ The exercise text, read as the good prompt.
- **Dependency:** `com.tngtech.archunit:archunit-junit5` 1.5.1, checked before adding it (latest release, HTTP 200, no CVEs).
- **`ArchitectureTest`:** 6 rules, each with a `because`:
  - controllers don't use repositories;
  - service, repository, entity and exception don't depend on the web layer;
  - endpoints don't accept or return `@Entity` classes, including generics such as `List<Team>`;
  - DTOs don't depend on entity classes;
  - exactly one `@ControllerAdvice` (`GlobalExceptionHandler`);
  - no field injection.
- **Proof:** a script broke each rule with a temporary class. Each time exactly that one rule failed (1 of 6), with a clear message. Then the class and its `.class` file were removed and all rules were green again.
- **Why not `git checkout`:** existing files were never edited, because a checkout would have destroyed uncommitted work.

`./mvnw verify`: 134 tests, 0 failures, coverage met.

---

<!-- Template for new entries:

## N. <short title>

```
<the exact prompt>
```

**Verdict:** ✅/🔧/❌ <one line: did it work, or what did you fix and change?>

-->

