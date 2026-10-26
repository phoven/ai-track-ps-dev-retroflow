package com.stackcraft.retroflow.exception;

/**
 * A request violates one of RetroFlow's business rules, e.g. adding feedback
 * to a closed retrospective or opening a second retrospective for a team.
 * Mapped to 409 Conflict. Subclasses refine the status (400, 403).
 *
 * Message format (standards §2.1): {@code <EntityType> <identifier>: <reason>},
 * with identifier {@code new} for entities that don't have an id yet.
 */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

    public RetroflowException(String entityType, Object identifier, String reason) {
        super(format(entityType, identifier, reason));
    }

    static String format(String entityType, Object identifier, String reason) {
        return "%s %s: %s".formatted(entityType, identifier == null ? "new" : identifier, reason);
    }

}

