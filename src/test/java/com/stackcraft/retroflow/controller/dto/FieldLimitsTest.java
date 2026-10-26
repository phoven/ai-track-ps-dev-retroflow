package com.stackcraft.retroflow.controller.dto;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The web-layer limits must match the entity column sizes, or a valid request could fail at the database. */
class FieldLimitsTest {

    @Test
    void fieldLimits_matchTheEntityColumnSizes() {
        // Assert
        assertThat(FieldLimits.MAX_NAME_LENGTH).isEqualTo(Team.MAX_NAME_LENGTH);
        assertThat(FieldLimits.MAX_TITLE_LENGTH).isEqualTo(Retrospective.MAX_TITLE_LENGTH);
    }

}

