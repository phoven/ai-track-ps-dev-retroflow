package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Matrix rows 3.8 and 3.9: an action item's priority can change while the retro is OPEN, not after close (D14, D1). */
@SpringBootTest
class ActionItemPriorityTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);

    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private ActionItemRepository actionItemRepository;

    private Retrospective retro;
    private ActionItem actionItem;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", RETRO_DATE, MEMBER);
        actionItem = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", MEMBER);
    }

    private Priority storedPriority() {
        return actionItemRepository.findById(actionItem.getId()).orElseThrow().getPriority();
    }

    @Test
    void changePriority_inOpenRetrospective_updatesPriority() {
        // Act
        ActionItem updated = feedbackService.changePriority(actionItem.getId(), Priority.LOW);

        // Assert
        assertThat(updated.getPriority()).isEqualTo(Priority.LOW);
        assertThat(storedPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void changePriority_ofCompletedItemInOpenRetrospective_isAllowed() {
        // Arrange
        feedbackService.completeActionItem(actionItem.getId());

        // Act
        feedbackService.changePriority(actionItem.getId(), Priority.LOW);

        // Assert
        assertThat(storedPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void changePriority_inClosedRetrospective_throwsExceptionAndKeepsPriority() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.changePriority(actionItem.getId(), Priority.LOW))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: cannot change the priority of an action item in a closed retrospective",
                        retro.getId());
        assertThat(storedPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void changePriority_withMissingPriority_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.changePriority(actionItem.getId(), null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("ActionItem %d: priority must not be empty", actionItem.getId());
    }

    @Test
    void changePriority_withUnknownId_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.changePriority(UNKNOWN_ID, Priority.LOW))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateActionItem_withPriorityAndCompleted_appliesBoth() {
        // Act
        ActionItem updated = feedbackService.updateActionItem(actionItem.getId(), Priority.MEDIUM, true);

        // Assert
        assertThat(updated.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    void updateActionItem_whenCompletionIsRejected_rollsBackPriorityChange() {
        // Arrange: completing twice is a 409 (D7)
        feedbackService.completeActionItem(actionItem.getId());

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.updateActionItem(actionItem.getId(), Priority.LOW, true))
                .isExactlyInstanceOf(RetroflowException.class);
        assertThat(storedPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void updateActionItem_withOnlyPriorityOnCompletedItem_changesPriorityAndKeepsItCompleted() {
        // Arrange: no "completed" in the request must not trigger the uncomplete check (D8)
        feedbackService.completeActionItem(actionItem.getId());

        // Act
        ActionItem updated = feedbackService.updateActionItem(actionItem.getId(), Priority.LOW, null);

        // Assert
        assertThat(updated.getPriority()).isEqualTo(Priority.LOW);
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    void updateActionItem_withCompletedFalseOnCompletedItem_throwsExceptionAndKeepsPriority() {
        // Arrange
        feedbackService.completeActionItem(actionItem.getId());

        // Act & Assert: D8, and the priority change in the same request is rolled back (D14)
        assertThatThrownBy(() -> feedbackService.updateActionItem(actionItem.getId(), Priority.LOW, false))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem %d: already completed, cannot be undone", actionItem.getId());
        assertThat(storedPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void updateActionItem_withCompletedFalseOnOpenItem_leavesItNotCompleted() {
        // Act
        ActionItem updated = feedbackService.updateActionItem(actionItem.getId(), null, false);

        // Assert
        assertThat(updated.getId()).isEqualTo(actionItem.getId());
        assertThat(updated.isCompleted()).isFalse();
    }

    @Test
    void updateActionItem_withoutFields_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.updateActionItem(actionItem.getId(), null, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("ActionItem %d: nothing to update: send priority and/or completed", actionItem.getId());
    }

}

