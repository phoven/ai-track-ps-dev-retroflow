package com.stackcraft.retroflow.controller;

import java.time.Instant;

/**
 * Error body required by the standards (§2.2):
 * {@code {"error": "EntityType id: reason", "status": 409, "timestamp": "..."}}.
 */
public record ApiError(String error, int status, Instant timestamp) {

    public static ApiError of(String error, int status) {
        return new ApiError(error, status, Instant.now());
    }

}

