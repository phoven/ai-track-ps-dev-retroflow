# RetroFlow – house rules for AI assistants

- **Stack:** Java 21, Spring Boot 3.3 (Jakarta EE, not `javax`), Maven, Spring Data JPA, H2 in-memory, JUnit 5 + JaCoCo (80% gate). Not Gradle, not PostgreSQL.
- **Source of truth:** `decisions.md` wins over defaults; `test-matrix.md` lists every rule to test. If something isn't decided there, flag it and ask: don't pick a default silently.
- **Code style:** constructor injection only (no field `@Autowired`), no Lombok (write explicit code), entities are classes, DTOs may be records. Package `com.stackcraft.retroflow.*`.
- **Layering:** controllers stay thin; business rules live in the service layer. Never return `null` for business errors: throw a domain exception from `exception/`, mapped centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`).
- **HTTP status:** 201 create, 200 read/update, 204 delete, 400 validation, 403 non-member actor, 404 unknown id, 409 business-rule violation, 500 with no internal details.
- **One-way state:** no setters for `status`/`completed`; use `close()` / `complete()`. Closed retros and completed action items never go back.
- **Validation:** validate all input with Bean Validation and return a meaningful message.
- **Tests:** one test per row in `test-matrix.md`, named after the rule. Test service rules directly and status codes via the controller.
- **Explain your choices:** when there are several options (inheritance strategy, fetch type, status code), explain the trade-off you made.
- **Before saying "done":** run `./mvnw test` (or `./mvnw verify`) and report the result. No placeholders or TODOs.
- **Prompt log:** add every user prompt word for word to `prompt-log.md`, with a one-line verdict: did it work as-is, or what was fixed and changed.
- **Never** change, weaken, or delete a test just to make it pass. If a test looks wrong, stop and ask first. Vendor tests stay as they are.
- **Never** add a dependency or plugin without checking that the artifact and version really exist (Maven Central). Say why it's needed.
- **Never** hard-code secrets, run the container as root, or use unpinned/over-permissioned GitHub Actions.

