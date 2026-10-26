## Plan: Vendor tests and enforcing the test-matrix rules

The three vendor tests should already compile and pass: every method they call exists on `RetrospectiveService` with a matching signature. I haven't run them, since I can't run commands in this mode. Most of `test-matrix.md` is already enforced in `FeedbackService`, `RetrospectiveService` and `TeamService`. What's left is a few loopholes, some rows with no test, and some decisions you haven't made yet.

**1. Methods the vendor tests call**

| Call in test | Status |
|---|---|
| `teamService.createTeam(String, List<String>)` | Exists, same signature |
| `createRetrospective(Long, String)` | Exists as the D13 overload |
| `closeRetrospective(Long)` | Exists as the D13 overload |
| `addFeedbackItem(Long, String, String, String)` | Exists; passes on to `FeedbackService` |
| `addActionItem(Long, content, priority, author)` | Exists, parameters in the order the test uses |
| `completeActionItem(Long)` / `uncompleteActionItem(Long)` | Exist; pass on to `FeedbackService` |

Nothing is missing. One risk: `InvalidInputException` and `NotTeamMemberException` are subclasses of `RetroflowException`. A vendor test that only checks `isInstanceOf(RetroflowException)` would also pass if a 400 or 403 error were thrown by mistake.

**2. Where each rule is enforced, which exception it throws, and whether it's tested**

| Row | Enforced in | Exception | Test? |
|---|---|---|---|
| 1.2 | `RetrospectiveService.open` | `RetroflowException` | ✅ vendor + service + controller |
| 1.3 | `open` (looks only for an OPEN retro) | – | ✅ |
| 1.4 | `TeamRepository.findWithLockById` (pessimistic lock) | `RetroflowException` | ❌ |
| 2.2 | `RetrospectiveService.close` | `RetroflowException` | ✅ controller |
| 2.3 | `Retrospective` has no status setter; there is no reopen route | – | ❌ (decision needed) |
| 3.1 / 3.2 | `FeedbackService.openRetrospective` | `RetroflowException` | ✅ |
| 3.3 / 3.4 | `FeedbackService.requireOpen` | `RetroflowException` | ✅ |
| 3.7 | `completeActionItem` (no check that the retro is open) | – | ✅ |
| 3.8 / 3.9 | Not enforced: `ActionItem.setPriority` is public | – | ❌ (O1 not decided) |
| 4.2 / 4.3 | `completeActionItem` / `uncompleteActionItem` | `RetroflowException` | ✅ |
| B.2 / B.3 | `TeamService.createTeam` / `validateMembers` | `RetroflowException` / `InvalidInputException` | ✅ controller only |
| B.4 / B.5 | `ServiceInput.requireMember` | `NotTeamMemberException` | ✅ |
| B.7 / B.8 | `FeedbackService.addFeedbackItem` | `InvalidInputException` | ✅ |
| B.10 | Not built: `ActionItemRepository` has no filter queries | – | ❌ |
| B.11 | `getRetrospective`, `getFeedbackItem`, `getActionItem`, `lockTeam`, `getTeam` | `ResourceNotFoundException` | ✅ partly |

### Steps
1. Run `./mvnw test` and confirm `RetroflowBusinessRulesTest` passes as it is. Don't edit it.
2. Add service tests for rows 1.2, 3.1 and 4.3 that check the exact exception class. This closes the subclass risk without touching the vendor tests.
3. Add a concurrency test for row 1.4 in [RetrospectiveServiceTest.java](src/test/java/com/stackcraft/retroflow/service/RetrospectiveServiceTest.java): two threads call `createRetrospective` at the same time, one should succeed and one should get a 409.
4. Build the B.10 filter: add a query to `ActionItemRepository`, a method in `FeedbackService`, and `GET /api/retrospectives/{id}/action-items`. Add one test for each filter combination.
5. Add service tests in [TeamServiceTest.java](src/test/java/com/stackcraft/retroflow/service/TeamServiceTest.java) for B.2 and B.3. They're currently only tested through the controller.
6. Update the Covered-by column in [test-matrix.md](test-matrix.md) and add an entry to [prompt-log.md](prompt-log.md).

### Further Considerations
1. **O1 (changing an action item's priority):** decide in [decisions.md](decisions.md) whether to add a service method that only works while the retro is open (409 once closed), or to declare it out of scope and remove the public `ActionItem.setPriority`.
2. **Row 2.3 (reopen):** write down that there is no reopen or update endpoint, so the expected status is 404 or 405. Or drop the row as impossible by design.
3. **Setters `FeedbackItem.setAuthor` and `Team.setName` / `Team.setMembers`:** D5 and D11 say these values can't change, so these public setters let code bypass those decisions. Do you want them removed?

This is a draft for review. Should anything change before someone implements it?

