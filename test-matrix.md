# RetroFlow: test matrix

One row for every business rule and every operation it covers. The rows are derived only from the
[brief](https://github.com/switchfully/ai-track-ps-dev/blob/main/retroflow-brief.md) and
[`decisions.md`](decisions.md) (Dx = team decision, Px = proposed default). Rows marked ⚠ need a
decision that isn't written down yet. Don't pick an answer; decide it in `decisions.md` first.

**Exceptions:** `RetroflowException` → 409 · `InvalidInputException` → 400 ·
`NotTeamMemberException` → 403 · `ResourceNotFoundException` → 404.
**Covered by:** the test that checks the row today. *Service* = service layer only (no endpoint yet).
**—** means there's no test yet: Block 2 starts there.

## A. The four core rules from the brief

**22 rows:** 4 + 5 + 9 + 4.

### Rule 1: a team can only have one OPEN retrospective at a time

| #   | Operation | Situation | Expected result | Exception | HTTP | Covered by |
|-----|-----------|-----------|-----------------|-----------|------|------------|
| 1.1 | Create retrospective `POST /api/teams/{id}/retrospectives` | Team has no retrospective | Created, status OPEN | – | 201 | `RetrospectiveControllerTest.createRetrospective_byMember_returns201` |
| 1.2 | Create retrospective | Team already has an OPEN retrospective | Rejected; existing retro unchanged | `RetroflowException` | 409 | Vendor `aTeam_cannotHaveTwoOpenRetrospectives`, `…_whenTeamAlreadyHasOpenRetro_returns409` |
| 1.3 | Create retrospective | Team's previous retrospective is CLOSED | Created, status OPEN | – | 201 | `RetrospectiveServiceTest.createRetrospective_afterPreviousClosed_opensNewOne` (service) |
| 1.4 | Create retrospective, two requests at the same time | Team has no OPEN retrospective (P7) | Exactly one created; the other rejected | `RetroflowException` | 201 + 409 | `RetrospectiveServiceTest.createRetrospective_concurrentRequests_opensExactlyOne` (service; 8 parallel requests, fails without the team lock) |

### Rule 2: a closed retrospective cannot be reopened

| #   | Operation | Situation | Expected result | Exception | HTTP | Covered by |
|-----|-----------|-----------|-----------------|-----------|------|------------|
| 2.1 | Close `PUT /api/retrospectives/{id}/close` | Retro is OPEN, `closedBy` is a member | Status CLOSED | – | 200 | `RetrospectiveControllerTest.closeRetrospective_byMember_returns200Closed` |
| 2.2 | Close | Retro is already CLOSED (D7) | Rejected, stays CLOSED | `RetroflowException` | 409 | `…closeRetrospective_whenAlreadyClosed_returns409` |
| 2.3 | Reopen `PATCH /api/retrospectives/{id}` `{"status": "OPEN"}` (D15) | Retro is CLOSED | Rejected, stays CLOSED. The entity has no status setter (D12) | `RetroflowException` | 409 | `RetrospectiveUpdateTest.reopenRetrospective_whenClosed_throwsExceptionAndStaysClosed`, `RetrospectiveUpdateControllerTest.reopenRetrospective_whenClosed_returns409` |
| 2.4 | Change title/date (PATCH, D15) | Retro is CLOSED | Rejected, values unchanged | `RetroflowException` | 409 | `RetrospectiveUpdateTest.updateRetrospective_whenClosed_throwsExceptionAndKeepsValues`, `…ControllerTest.updateRetrospective_whenClosed_returns409` |
| 2.5 | Change title/date (PATCH, D15) | Retro is OPEN | Updated | – | 200 | `RetrospectiveUpdateTest.updateRetrospective_whenOpen_changesTitleAndDate`, `…ControllerTest.updateRetrospective_whenOpen_returns200` |

### Rule 3: feedback items in a CLOSED retrospective cannot be added, modified or deleted

| #   | Operation | Situation | Expected result | Exception | HTTP | Covered by |
|-----|-----------|-----------|-----------------|-----------|------|------------|
| 3.1 | Add feedback (WENT_WELL / NEEDS_IMPROVEMENT) | Retro is CLOSED | Rejected | `RetroflowException` | 409 | Vendor `closingARetrospective_preventsAddingFeedback` (service) |
| 3.2 | Add action item `POST /api/retrospectives/{id}/action-items` (D16) | Retro is CLOSED | Rejected | `RetroflowException` | 409 | `FeedbackServiceTest.addActionItem_toClosedRetrospective_throwsException` (service), `RetrospectiveControllerTest.addActionItem_toClosedRetrospective_returns409` |
| 3.3 | Modify feedback content (PUT, D5) | Retro is CLOSED | Rejected, content unchanged | `RetroflowException` | 409 | `FeedbackServiceTest.updateFeedbackContent_inClosedRetrospective_throwsException` (service) |
| 3.4 | Delete feedback item (DELETE, D5) | Retro is CLOSED | Rejected, item still exists | `RetroflowException` | 409 | `FeedbackServiceTest.deleteFeedbackItem_inClosedRetrospective_throwsExceptionAndKeepsItem` (service) |
| 3.5 | Modify feedback content | Retro is OPEN | Content updated | – | 200 | `FeedbackServiceTest.updateFeedbackContent_inOpenRetrospective_updatesContent` (service) |
| 3.6 | Delete feedback item | Retro is OPEN | Item removed | – | 204 | `FeedbackServiceTest.deleteFeedbackItem_inOpenRetrospective_removesIt` (service) |
| 3.7 | Complete an action item | Retro is CLOSED (D1: the only change allowed) | Completed | – | 200 | `ActionItemCompletionTest.completeActionItem_afterRetrospectiveClosed_isAllowed` (service) |
| 3.8 | Change an action item's priority `PATCH /api/action-items/{id}` `{"priority": …}` (D14) | Retro is CLOSED (D1: frozen) | Rejected, priority unchanged | `RetroflowException` | 409 | `ActionItemPriorityTest.changePriority_inClosedRetrospective_throwsExceptionAndKeepsPriority`, `ActionItemControllerTest.changePriority_inClosedRetrospective_returns409` |
| 3.9 | Change an action item's priority (D14) | Retro is OPEN (also when the item is completed) | Priority updated | – | 200 | `ActionItemPriorityTest.changePriority_inOpenRetrospective_updatesPriority`, `…_ofCompletedItemInOpenRetrospective_isAllowed`, `ActionItemControllerTest.changePriority_inOpenRetrospective_returns200` |

### Rule 4: completed action items cannot be uncompleted

| #   | Operation | Situation | Expected result | Exception | HTTP | Covered by |
|-----|-----------|-----------|-----------------|-----------|------|------------|
| 4.1 | Complete `PATCH /api/action-items/{id}` `{"completed": true}` | Not completed | Completed | – | 200 | `ActionItemCompletionTest.completeActionItem_inOpenRetrospective_marksItCompleted` (service), `ActionItemControllerTest.completeActionItem_returns200ThenRepeatReturns409` |
| 4.2 | Complete | Already completed (D7) | Rejected | `RetroflowException` | 409 | `ActionItemCompletionTest.completeActionItem_whenAlreadyCompleted_throwsException` (service), `ActionItemControllerTest.completeActionItem_returns200ThenRepeatReturns409` |
| 4.3 | Uncomplete `{"completed": false}` (D8) | Completed | Rejected, stays completed | `RetroflowException` | 409 | Vendor `completedActionItem_cannotBeUncompleted` (service), `ActionItemControllerTest.uncompleteActionItem_whenCompletedReturns409_whenNotCompletedReturns200` |
| 4.4 | Uncomplete `{"completed": false}` (D8) | Not completed | No change | – | 200 | `ActionItemCompletionTest.uncompleteActionItem_whenNotCompleted_leavesItUnchanged` (service), `ActionItemControllerTest.uncompleteActionItem_whenCompletedReturns409_whenNotCompletedReturns200` |

## B. Other rules from the brief and decisions.md

| #    | Rule | Operation | Situation | Expected result | Exception | HTTP | Covered by |
|------|------|-----------|-----------|-----------------|-----------|------|------------|
| B.1  | Team needs ≥ 1 member (brief) | Create team | `members` empty or missing | Rejected | validation | 400 | `TeamControllerTest.createTeam_withBlankNameAndNoMembers_returns400WithBothFields` |
| B.2  | Team names unique, case-insensitive (D10) | Create team | Name taken, any case | Rejected | `RetroflowException` | 409 | `TeamControllerTest.createTeam_withTakenName_returns409` |
| B.3  | Member names unique in a team (D10) | Create team | `["Alice", "alice"]` | Rejected | `InvalidInputException` | 400 | `TeamControllerTest.createTeam_withDuplicateMembers_returns400` |
| B.4  | Only members create retros (D3) | Create retrospective | `createdBy` not a member | Rejected | `NotTeamMemberException` | 403 | `…createRetrospective_byNonMember_returns403` |
| B.5  | Only members close retros (D3) | Close retrospective | `closedBy` not a member | Rejected, stays OPEN | `NotTeamMemberException` | 403 | `…closeRetrospective_byNonMemberOrWithoutClosedBy_returns403Or400` |
| B.6  | Date required, any date (D4) | Create retrospective | Date missing or not `yyyy-MM-dd` | Rejected | validation | 400 | `…createRetrospective_withMissingOrInvalidDate_returns400` |
| B.7  | Action item priority optional (D6) | Add `ACTION_ITEM` without priority | Created with MEDIUM | – | 201 | `FeedbackServiceTest.addFeedbackItem_withActionItemTypeAndNoPriority_createsMediumActionItem` (service) |
| B.8  | Priority only for action items (D6) | Add WENT_WELL with a priority | Rejected | `InvalidInputException` | 400 | `FeedbackServiceTest.addFeedbackItem_withPriorityForNonActionType_throwsInvalidInput` (service) |
| B.9  | Author not checked against the team (D2) | Add feedback | Author isn't a member | Created | – | 201 | `FeedbackServiceTest.addFeedbackItem_toOpenRetrospective_persistsTrimmedValues` (service) |
| B.10 | Filter action items per retro (D9) | `GET /api/retrospectives/{id}/action-items?priority=&completed=` | Each filter alone, both, neither | Matching items only; invalid filter value → 400; unknown retro → 404 | – | 200 | `ActionItemFilterTest` (service), `RetrospectiveControllerTest.listActionItems_*` |
| B.11 | Unknown ids | Any operation | Id doesn't exist | Rejected | `ResourceNotFoundException` | 404 | Controller and service tests (team, retrospective, feedback item, action item) |
| B.12 | Only members update retros (D15) | Update retrospective | `updatedBy` not a member | Rejected, unchanged | `NotTeamMemberException` | 403 | `RetrospectiveUpdateTest.updateRetrospective_byNonMember_throwsNotTeamMember`, `…ControllerTest.updateRetrospective_byNonMember_returns403` |

## Summary for Block 2

- **Not tested at all:** none.
- **Service-only:** 3.1–3.7, B.7–B.9 need endpoint tests once the feedback endpoints exist
  (`POST/GET /api/retrospectives/{id}/feedback-items`, `PUT/DELETE /api/feedback-items/{id}`).
- **Needs a decision first:** none (O2 in `decisions.md` is about the vendor tests, not a matrix row).
- **Exact exception class:** the vendor tests only check `isInstanceOf(RetroflowException)`, which a 400/403
  subclass would also satisfy. Rows 1.2, 2.2, 3.1–3.4, 4.2, 4.3 and B.2 also have a service test with
  `isExactlyInstanceOf(RetroflowException.class)`.

