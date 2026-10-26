package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotTeamMemberException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Matrix rows 2.3–2.5 and B.12: updating a retrospective (D15). A CLOSED one is frozen and can't be reopened. */
@SpringBootTest
class RetrospectiveUpdateTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final String OUTSIDER = "Mallory";
    private static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);
    private static final LocalDate NEW_DATE = LocalDate.of(2026, 10, 28);

    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;

    private Retrospective retro;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", RETRO_DATE, MEMBER);
    }

    private Retrospective reloaded() {
        return retrospectiveService.getRetrospective(retro.getId());
    }

    @Test
    void updateRetrospective_whenOpen_changesTitleAndDate() {
        // Act
        retrospectiveService.updateRetrospective(retro.getId(), " Sprint 7 (moved) ", NEW_DATE, null, MEMBER);

        // Assert
        assertThat(reloaded().getTitle()).isEqualTo("Sprint 7 (moved)");
        assertThat(reloaded().getDate()).isEqualTo(NEW_DATE);
    }

    @Test
    void updateRetrospective_whenOpen_returnsTheUpdatedRetrospective() {
        // Act
        Retrospective updated = retrospectiveService.updateRetrospective(retro.getId(), "Renamed", NEW_DATE, null, MEMBER);

        // Assert: the controller builds its response from this return value
        assertThat(updated.getId()).isEqualTo(retro.getId());
        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getDate()).isEqualTo(NEW_DATE);
        assertThat(updated.isOpen()).isTrue();
    }

    @Test
    void updateRetrospective_withOnlyDate_keepsTitle() {
        // Act
        retrospectiveService.updateRetrospective(retro.getId(), null, NEW_DATE, RetrospectiveStatus.OPEN, MEMBER);

        // Assert
        assertThat(reloaded().getTitle()).isEqualTo("Sprint 7");
        assertThat(reloaded().getDate()).isEqualTo(NEW_DATE);
    }

    @Test
    void reopenRetrospective_whenClosed_throwsExceptionAndStaysClosed() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(
                retro.getId(), null, null, RetrospectiveStatus.OPEN, MEMBER))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: closed, cannot be reopened", retro.getId());
        assertThat(reloaded().getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void updateRetrospective_whenClosed_throwsExceptionAndKeepsValues() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(retro.getId(), "Renamed", NEW_DATE, null, MEMBER))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: closed, cannot be modified", retro.getId());
        assertThat(reloaded().getTitle()).isEqualTo("Sprint 7");
        assertThat(reloaded().getDate()).isEqualTo(RETRO_DATE);
    }

    @Test
    void updateRetrospective_withStatusClosed_throwsInvalidInputAndStaysOpen() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(
                retro.getId(), null, null, RetrospectiveStatus.CLOSED, MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("use the close operation");
        assertThat(reloaded().isOpen()).isTrue();
    }

    @Test
    void updateRetrospective_withNothingOrBlankTitle_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(retro.getId(), null, null, null, MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("nothing to update");
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(retro.getId(), " ", null, null, MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Retrospective %d: title must not be blank", retro.getId());
    }

    @Test
    void updateRetrospective_byNonMember_throwsNotTeamMember() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(retro.getId(), "Renamed", null, null, OUTSIDER))
                .isInstanceOf(NotTeamMemberException.class);
        assertThat(reloaded().getTitle()).isEqualTo("Sprint 7");
    }

    @Test
    void updateRetrospective_withUnknownId_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.updateRetrospective(UNKNOWN_ID, "Renamed", null, null, MEMBER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}

