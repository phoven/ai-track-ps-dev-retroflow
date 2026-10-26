## Plan: Web layer – remove domain types, one error format

No controller accepts or returns an entity: all 9 endpoints already use DTO records. But the DTO classes import entities (`Team`, `Retrospective`, `ActionItem`) for their `from()` mappers and length constants, and they expose the domain enums `Priority` and `RetrospectiveStatus` as JSON fields. There is already only one `@RestControllerAdvice` (`GlobalExceptionHandler`) using `ApiError`. Some errors still escape that format, though. I can't edit files in this mode, so this is the plan.

### Steps
1. Move all entity→DTO mapping out of the records in [TeamDtos.java](src/main/java/com/stackcraft/retroflow/controller/dto/TeamDtos.java), [RetrospectiveDtos.java](src/main/java/com/stackcraft/retroflow/controller/dto/RetrospectiveDtos.java) and [ActionItemDtos.java](src/main/java/com/stackcraft/retroflow/controller/dto/ActionItemDtos.java) into one new `controller/mapper/ResponseMapper`. The DTOs then have no `entity` imports.
2. Replace the `Team.MAX_NAME_LENGTH` and `Retrospective.MAX_TITLE_LENGTH` references in the request DTOs with a new web-layer `controller/dto/FieldLimits` class holding the same values (100 and 200), with a test asserting they match the entity constants.
3. Decide on the enums (see consideration 1). If they go, add web-layer enums `PriorityDto` and `RetrospectiveStatusDto` and map them in `ResponseMapper`, and in the controllers for `RetrospectiveController.listActionItems`, `ActionItemController` and the PATCH bodies.
4. Make the error format truly single:
   - **Errors outside a controller:** add a new `controller/ApiErrorController implements ErrorController` on `/error`, so failures in filters or the servlet container also return `ApiError` instead of Spring Boot's `{timestamp, status, error, path}`.
   - **Spring's own client errors:** in `GlobalExceptionHandler.unexpected`, give them the `"Request new: <reason>"` message instead of only the reason phrase, e.g. a missing parameter or wrong media type.
   - **Unknown enum in a JSON body:** in `GlobalExceptionHandler.unreadableBody`, name the field and the allowed values (pending O5).
5. Map persistence races in `GlobalExceptionHandler` to the agreed codes instead of a 500 (see consideration 2):
   - `DataIntegrityViolationException`, e.g. two teams with the same name at the same time, → 409.
   - `PessimisticLockingFailureException` → 409.
6. Add web-layer tests with mocked services:
   - **Every error path** returns `{error, status, timestamp}`: 400 body/param/type, 403, 404 route/id, 405, 409, 500, and `/error`.
   - **Without the `entity` package**, every response body has the same JSON fields as before.
   - **Last,** run `./mvnw verify` and add a prompt-log entry.

### Further Considerations
1. **Enums:** `Priority` and `RetrospectiveStatus` are not entities, but they live in `entity`. Option A: keep them (simple; JSON stays identical). Option B: mirror them in the web layer (strict separation, but duplication and mapping). I recommend A plus an ArchUnit exception for the enums.
2. **Race-condition status codes:** 409 for a duplicate name or a lock timeout isn't in `decisions.md` yet. Confirm it, or keep 500 and add it to the decisions?
3. **Actuator endpoints** (`/actuator/**`) keep Spring's own error format. Should they stay out of scope?

