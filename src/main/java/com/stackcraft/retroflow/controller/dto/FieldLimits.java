package com.stackcraft.retroflow.controller.dto;

/**
 * Length limits for request validation (P1). Kept in the web layer so the request DTOs don't
 * depend on entity classes; {@code FieldLimitsTest} checks they match the entity column sizes.
 */
public final class FieldLimits {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_CONTENT_LENGTH = 2000;

    private FieldLimits() {
    }

}

