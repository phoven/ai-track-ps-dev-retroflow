package com.stackcraft.retroflow.exception;

/** Invalid input detected by the service layer (blank values, duplicates, unknown enum values). Mapped to 400. */
public class InvalidInputException extends RetroflowException {

    public InvalidInputException(String entityType, Object identifier, String reason) {
        super(entityType, identifier, reason);
    }

}

