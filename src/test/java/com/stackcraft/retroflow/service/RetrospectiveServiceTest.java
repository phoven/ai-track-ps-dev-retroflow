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

@SpringBootTest
class RetrospectiveServiceTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final String OTHER_MEMBER = "Heidi";
    private static final String OUTSIDER = "Mallory";
    private static final LocalDate LAST_SPRINT = LocalDate.of(2026, 10, 12);
    private static final LocalDate THIS_SPRINT = LocalDate.of(2026, 10, 26);

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    private Team team;

    @BeforeEach
    void setUp() {
        team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER, OTHER_MEMBER));
    }

    @Test
    void createRetrospective_byTeamMember_opensRetrospectiveWithGivenDate() {
        // Act
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), " Sprint 7 ", LAST_SPRINT, MEMBER);

        // Assert
        assertThat(retro.getTitle()).isEqualTo("Sprint 7");
        assertThat(retro.getDate()).isEqualTo(LAST_SPRINT);
        assertThat(retro.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    void createRetrospective_whenTeamAlreadyHasOpenRetro_throwsException() {
        // Arrange
        Retrospective open = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Sprint 8", THIS_SPRINT, MEMBER))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Team %d: already has an open retrospective (Retrospective %d)", team.getId(), open.getId());
    }

    @Test
    void createRetrospective_afterPreviousClosed_opensNewOne() {
        // Arrange
        Retrospective first = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);
        retrospectiveService.closeRetrospective(first.getId(), MEMBER);

        // Act
        Retrospective second = retrospectiveService.createRetrospective(team.getId(), "Sprint 8", THIS_SPRINT, MEMBER);

        // Assert
        assertThat(second.isOpen()).isTrue();
    }

    @Test
    void createRetrospective_byNonMember_throwsNotTeamMember() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, OUTSIDER))
                .isInstanceOf(NotTeamMemberException.class)
                .hasMessage("Team %d: 'Mallory' is not a member of this team", team.getId());
    }

    @Test
    void createRetrospective_byMemberInDifferentCase_isAllowed() {
        // Act
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, " grace ");

        // Assert
        assertThat(retro.isOpen()).isTrue();
    }

    @Test
    void createRetrospective_withBlankTitleOrMissingDate_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), " ", LAST_SPRINT, MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Retrospective new: title must not be blank");
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Sprint 7", null, MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Retrospective new: date must not be empty");
    }

    @Test
    void createRetrospective_forUnknownTeam_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(UNKNOWN_ID, "Sprint 7", LAST_SPRINT, MEMBER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRetrospective_vendorOverload_usesTodaysDate() {
        // Act
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7");

        // Assert
        assertThat(retro.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void closeRetrospective_byTeamMember_closesIt() {
        // Arrange
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);

        // Act
        Retrospective closed = retrospectiveService.closeRetrospective(retro.getId(), OTHER_MEMBER);

        // Assert
        assertThat(closed.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
        assertThat(retrospectiveService.getRetrospective(retro.getId()).isOpen()).isFalse();
    }

    @Test
    void closeRetrospective_whenAlreadyClosed_throwsException() {
        // Arrange
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(retro.getId(), MEMBER))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: already closed, cannot be closed again", retro.getId());
    }

    @Test
    void closeRetrospective_byNonMember_throwsNotTeamMemberAndStaysOpen() {
        // Arrange
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(retro.getId(), OUTSIDER))
                .isInstanceOf(NotTeamMemberException.class);
        assertThat(retrospectiveService.getRetrospective(retro.getId()).isOpen()).isTrue();
    }

    @Test
    void closeRetrospective_withUnknownId_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(UNKNOWN_ID, MEMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Retrospective %d: not found", UNKNOWN_ID);
    }

    @Test
    void listRetrospectives_withSeveralRetros_returnsNewestFirst() {
        // Arrange
        Retrospective older = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LAST_SPRINT, MEMBER);
        retrospectiveService.closeRetrospective(older.getId(), MEMBER);
        Retrospective newer = retrospectiveService.createRetrospective(team.getId(), "Sprint 8", THIS_SPRINT, MEMBER);

        // Act
        List<Retrospective> retros = retrospectiveService.listRetrospectives(team.getId());

        // Assert
        assertThat(retros).extracting(Retrospective::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void listRetrospectives_forUnknownTeam_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.listRetrospectives(UNKNOWN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}

