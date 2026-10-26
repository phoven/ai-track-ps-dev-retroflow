package com.stackcraft.retroflow;

import java.util.UUID;

/** Shared test data helpers. */
public final class TestData {

    private TestData() {
    }

    /** Team names are unique (D10) and all tests share one in-memory database. */
    public static String uniqueTeamName() {
        return "Team " + UUID.randomUUID();
    }

}

