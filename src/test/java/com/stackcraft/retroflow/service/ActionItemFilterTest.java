package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Matrix row B.10: action items are filtered per retrospective by priority and/or completed (D9). */
@SpringBootTest
class ActionItemFilterTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);

    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;
    @Autowired
    private FeedbackService feedbackService;

    private Retrospective retro;
    private ActionItem highOpen;
    private ActionItem highDone;
    private ActionItem lowOpen;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", RETRO_DATE, MEMBER);
        highOpen = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", MEMBER);
        highDone = retrospectiveService.addActionItem(retro.getId(), "Pin actions", "HIGH", MEMBER);
        lowOpen = retrospectiveService.addActionItem(retro.getId(), "Tidy README", "LOW", MEMBER);
        retrospectiveService.addFeedbackItem(retro.getId(), "Pairing helped", "WENT_WELL", MEMBER);
        feedbackService.completeActionItem(highDone.getId());
    }

    private List<Long> idsOf(Priority priority, Boolean completed) {
        return feedbackService.listActionItems(retro.getId(), priority, completed).stream().map(ActionItem::getId).toList();
    }

    @Test
    void listActionItems_withoutFilters_returnsOnlyActionItemsOfThatRetrospective() {
        // Arrange: another team's retrospective with its own action item must not leak in
        Team otherTeam = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        Retrospective otherRetro = retrospectiveService.createRetrospective(otherTeam.getId(), "Other", RETRO_DATE, MEMBER);
        retrospectiveService.addActionItem(otherRetro.getId(), "Not mine", "HIGH", MEMBER);

        // Act & Assert
        assertThat(idsOf(null, null)).containsExactly(highOpen.getId(), highDone.getId(), lowOpen.getId());
    }

    @Test
    void listActionItems_byPriority_returnsOnlyThatPriority() {
        // Act & Assert
        assertThat(idsOf(Priority.HIGH, null)).containsExactly(highOpen.getId(), highDone.getId());
        assertThat(idsOf(Priority.MEDIUM, null)).isEmpty();
    }

    @Test
    void listActionItems_byCompleted_returnsOnlyThatStatus() {
        // Act & Assert
        assertThat(idsOf(null, true)).containsExactly(highDone.getId());
        assertThat(idsOf(null, false)).containsExactly(highOpen.getId(), lowOpen.getId());
    }

    @Test
    void listActionItems_byPriorityAndCompleted_combinesBothFilters() {
        // Act & Assert
        assertThat(idsOf(Priority.HIGH, false)).containsExactly(highOpen.getId());
        assertThat(idsOf(Priority.LOW, true)).isEmpty();
    }

    @Test
    void listActionItems_ofClosedRetrospective_stillWorks() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThat(idsOf(null, false)).containsExactly(highOpen.getId(), lowOpen.getId());
    }

    @Test
    void listActionItems_ofUnknownRetrospective_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.listActionItems(UNKNOWN_ID, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Retrospective %d: not found", UNKNOWN_ID);
    }

}

