package com.stackcraft.retroflow.exception;

/**
 * Thrown when a referenced resource (team, retrospective, action item) does
 * not exist. Kept separate from {@link RetroflowException} so it can be
 * mapped to 404 instead of a business-rule error.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super("%s %d not found".formatted(resource, id));
    }

}

