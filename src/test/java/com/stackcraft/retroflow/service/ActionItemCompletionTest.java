package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
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

/** Completing action items: one-way (brief), allowed after close (D1), repeat = 409 (D7), uncomplete via D8. */
@SpringBootTest
class ActionItemCompletionTest {

    private static final String MEMBER = "Grace";
    private static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);

    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;
    @Autowired
    private FeedbackService feedbackService;

    private Retrospective retro;
    private ActionItem actionItem;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", RETRO_DATE, MEMBER);
        actionItem = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", MEMBER);
    }

    @Test
    void completeActionItem_inOpenRetrospective_marksItCompleted() {
        // Act
        ActionItem completed = feedbackService.completeActionItem(actionItem.getId());

        // Assert
        assertThat(completed.isCompleted()).isTrue();
    }

    @Test
    void completeActionItem_afterRetrospectiveClosed_isAllowed() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act
        ActionItem completed = feedbackService.completeActionItem(actionItem.getId());

        // Assert
        assertThat(completed.isCompleted()).isTrue();
    }

    @Test
    void completeActionItem_whenAlreadyCompleted_throwsException() {
        // Arrange
        feedbackService.completeActionItem(actionItem.getId());

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.completeActionItem(actionItem.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem %d: already completed", actionItem.getId());
    }

    @Test
    void uncompleteActionItem_whenCompleted_throwsException() {
        // Arrange
        feedbackService.completeActionItem(actionItem.getId());

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.uncompleteActionItem(actionItem.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem %d: already completed, cannot be undone", actionItem.getId());
    }

    @Test
    void uncompleteActionItem_whenNotCompleted_leavesItUnchanged() {
        // Act
        ActionItem result = feedbackService.uncompleteActionItem(actionItem.getId());

        // Assert
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void completeActionItem_onPlainFeedbackItem_throwsNotFound() {
        // Arrange
        FeedbackItem feedback = retrospectiveService.addFeedbackItem(retro.getId(), "Nice", "WENT_WELL", MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.completeActionItem(feedback.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ActionItem %d: not found", feedback.getId());
    }

    @Test
    void updateFeedbackContent_ofActionItemInClosedRetrospective_throwsException() {
        // Arrange: completion is allowed after close (D1), but content stays frozen
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.updateFeedbackContent(actionItem.getId(), "Changed"))
                .isExactlyInstanceOf(RetroflowException.class);
    }

}

