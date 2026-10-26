package com.stackcraft.retroflow.exception;

/**
 * A referenced resource does not exist. Mapped to 404. Kept separate from
 * {@link RetroflowException} because a missing resource is not a rule violation.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entityType, Long id) {
        super(RetroflowException.format(entityType, id, "not found"));
    }

}

