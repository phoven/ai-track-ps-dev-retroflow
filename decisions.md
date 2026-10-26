# RetroFlow: decisions

Our team's interpretation of everything [the brief](https://github.com/switchfully/ai-track-ps-dev/blob/main/retroflow-brief.md)
leaves open. When code, tests or `test-matrix.md` disagree with this file, this file wins. Change the
decision here first, then the code.

Status codes and error messages follow the StackCraft coding standards (§2.1, §2.2, §4.1) unless a
decision below says otherwise.

## Team decisions

| #   | Topic | Decision | Why |
|-----|-------|----------|-----|
| D1  | Completing an action item after its retrospective is closed | **Allowed.** Completing is the *only* change allowed after close. Content and priority stay frozen. | Action items are done after the retro. The brief's goal is to track "whether those action items were ever resolved". |
| D2  | Must a feedback author be a team member? | **No.** Any non-blank name is accepted. | Without authentication, a name check adds little. |
| D3  | Who creates/closes a retrospective | The request names the actor (`createdBy` / `closedBy`). The actor **must be a member** of the team, otherwise **403 Forbidden**. | The brief says "any team member can…". 403 is a deliberate exception to the standards' status table. |
| D4  | Retrospective date | **Provided by the client, required, any date** (past or future). | Teams also record retros held earlier. |
| D5  | Editing/deleting feedback | **PUT** (content only) and **DELETE** for feedback items, **only while the retro is OPEN**. In a CLOSED retro: **409**. Type and author can't be changed. | Without edit and delete operations, the "cannot be modified or deleted" rule protects nothing. |
| D6  | Priority of a new action item | **Optional, default `MEDIUM`.** A priority sent with a non-action type is rejected (**400**). | Keeps the common case short. |
| D7  | Repeating a one-way operation (close a CLOSED retro, complete a COMPLETED item) | **409 Conflict.** | Surfaces client bugs; it is a business-rule violation. |
| D8  | "Completed action items cannot be uncompleted" in the API | Generic update `PATCH /api/action-items/{id}` with `{"completed": false}`. Rejected with **409** if the item is completed. A no-op (200) if it isn't. | One update endpoint. The rule is enforced in the service. |
| D9  | Scope of action-item filtering | **Per retrospective.** `priority` and `completed` are both optional and can be combined. | Matches how a retro is reviewed. |
| D10 | Uniqueness | **Team names are unique, case-insensitive (409).** Member names are unique within a team, case-insensitive (**400**). | Avoids "which Alpha team?" and ambiguous members. |
| D11 | Changing team members | **Out of scope today:** teams are create + retrieve only. | The brief only asks for create + retrieve. |
| D12 | Entity setters | **No public setters at all.** Changes go through methods named after what they do: `close()`, `complete()`, `rename()`, `reschedule()`, `editContent()`, `changePriority()`. The service checks the rules before calling them. | Public setters would let code bypass the one-way and closed-retro rules. This deviates from Prompt 3. |
| D13 | Vendor test signatures | The vendor test needs `createRetrospective(teamId, title)` and `closeRetrospective(id)`. These stay as **overloads: date = today, no actor check**, used only by the vendor tests. The API always calls the full versions (date + actor). | We may not change the vendor tests. **Known gap; raise with the coach.** |
| D14 | Changing an action item's priority (was O1) | **Allowed while the retro is OPEN**, also for a completed item, through the D8 endpoint `PATCH /api/action-items/{id}` `{"priority": "HIGH"}`. In a CLOSED retro: **409** (D1: priority is frozen). `priority` and `completed` can be sent together and are applied in one transaction: all or nothing. A body with neither: **400**. | Team decision: priorities change as the sprint goes on. One update endpoint (D8). |
| D15 | Updating a retrospective (matrix row 2.3) | **`PATCH /api/retrospectives/{id}`** with optional `title`, `date`, `status` and a required `updatedBy` (a member, otherwise **403**, like D3). Only while **OPEN**. CLOSED: **409** (`status: OPEN` = reopen attempt, anything else = "cannot be modified"). `status: CLOSED` is **400**: closing goes through `PUT …/close`. Nothing to change: **400**. | Team decision: titles and dates need fixing. Sending `status` gives the "cannot be reopened" rule a real request to reject. |
| D16 | Adding an action item | **`POST /api/retrospectives/{id}/action-items`** with `content`, `author` (any name, D2) and an optional `priority` (D6). Returns **201** with the action item. CLOSED: **409**. Plain feedback items still wait for O3. | The UI needs to add action items now; this path matches the existing `GET` and doesn't decide O3/O4. |

## HTTP status policy

| Situation | Status |
|---|---|
| Resource created | 201 Created |
| Successful read, or update with a body | 200 OK |
| Successful delete | 204 No Content |
| Validation failure (missing or blank fields, unknown enum value, malformed JSON or id) | 400 Bad Request |
| Actor is not a member of the team (D3) | 403 Forbidden |
| Unknown id | 404 Not Found |
| Business-rule violation (second open retro, closed retro, repeat of a one-way operation, duplicate team name) | 409 Conflict |
| Anything unexpected | 500, with no internal details in the response |

Error body (standards §2.2): `{"error": "<EntityType> <id>: <reason>", "status": 409, "timestamp": "…"}`.
Exception messages use the format `<EntityType> <identifier>: <reason>` (standards §2.1). Before
an entity has an id, the identifier is `new`.

## Proposed defaults (decided by the AI, not yet confirmed by the team)

Small choices nobody has decided yet. Change them here if you disagree.

| #  | Topic | Proposal |
|----|-------|----------|
| P1 | Field limits | Team and member name ≤ 100 characters, title ≤ 200, feedback content ≤ 2000. Leading and trailing whitespace is trimmed. |
| P2 | List order | Retrospectives: newest date first. Feedback items: in the order they were added. |
| P3 | Retrospective titles | No uniqueness rule. |
| P4 | Deleting teams or retrospectives | Out of scope. |
| P5 | Who may edit or delete feedback | Anyone (no authentication, and the author isn't checked, consistent with D2). |
| P6 | Package structure | Stay layer-first (`controller`, `service`, …). The standards (§1.1) grandfather existing projects. |
| P7 | Concurrency of the one-open rule | Checked under a row lock on the team, so two concurrent requests can't both open a retro. |
| P8 | One error format everywhere | `GlobalExceptionHandler` is the only `@RestControllerAdvice`. `ApiErrorController` replaces Spring Boot's `/error`, so errors outside the controllers (filters, servlet container) also return `{"error", "status", "timestamp"}`. |
| P9 | Entities in the web layer | Request and response DTOs contain no entity classes. Mapping lives only in `controller/mapper/ResponseMapper`. The enums `Priority` and `RetrospectiveStatus` are value types and stay in the JSON contract as they are (no web-layer copies). |
| P10 | Unknown enum value in a JSON body (was O5) | 400 with the field and the allowed values: `Request new: 'URGENT' is not a valid priority (allowed: [LOW, MEDIUM, HIGH])`. |
| P11 | Races caught by the database (same new team name, lock timeout) | 409 `Request new: conflicts with a concurrent change, please try again`. SQL details are logged, never returned. |
| P12 | Actuator endpoints (`/actuator/**`) | Out of scope: they keep Spring's own format. |

## Still open

| # | Question | Needed for |
|---|---|---|
| O2 | D13: is the vendor-test overload acceptable, or should the vendor tests be updated with the coach's approval? The overloads `createRetrospective(teamId, title)` and `closeRetrospective(id)` skip the member check (D3) and use today's date (D4). The API never calls them, but they're public service methods. | Closing the gap in D3/D4 |
| O3 | Feedback endpoints: the brief requires "add" and "retrieve per retrospective", D5 adds edit (PUT) and delete. Which URLs? Proposal: `POST/GET /api/retrospectives/{id}/feedback-items`, `PUT/DELETE /api/feedback-items/{id}`. Listing needs a new service method (pair AC). | Feedback controller (Block 2, pair BD) |
| O4 | Shape of a feedback item in a list: one response with `type` plus `priority`/`completed` that are `null` for plain feedback, or leave those fields out for non-action items? | Feedback list response |
| O6 | Unknown query parameters (e.g. `?prio=HIGH`) are silently ignored and return the unfiltered list. Accept that, or reject with 400? | Action-item filter contract |

