package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
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
class FeedbackServiceTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final String NON_MEMBER_AUTHOR = "Visiting coach"; // authors aren't checked (D2)
    private static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);

    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private FeedbackItemRepository feedbackItemRepository;

    private Retrospective retro;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", RETRO_DATE, MEMBER);
    }

    @Test
    void addFeedbackItem_toOpenRetrospective_persistsTrimmedValues() {
        // Act
        FeedbackItem item = feedbackService.addFeedbackItem(
                retro.getId(), " Pairing helped ", FeedbackType.WENT_WELL, NON_MEMBER_AUTHOR, null);

        // Assert
        assertThat(item.getId()).isNotNull();
        assertThat(item.getContent()).isEqualTo("Pairing helped");
        assertThat(item.getAuthor()).isEqualTo(NON_MEMBER_AUTHOR);
    }

    @Test
    void addFeedbackItem_toClosedRetrospective_throwsException() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(
                retro.getId(), "Too late", FeedbackType.NEEDS_IMPROVEMENT, MEMBER, null))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: cannot add feedback to a closed retrospective", retro.getId());
    }

    @Test
    void addFeedbackItem_withActionItemTypeAndNoPriority_createsMediumActionItem() {
        // Act
        FeedbackItem item = retrospectiveService.addFeedbackItem(retro.getId(), "Fix CI", "action_item", MEMBER);

        // Assert
        assertThat(item).isInstanceOf(ActionItem.class);
        assertThat(((ActionItem) item).getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void addFeedbackItem_withPriorityForNonActionType_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(
                retro.getId(), "Nice", FeedbackType.WENT_WELL, MEMBER, Priority.HIGH))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("priority is only allowed for type ACTION_ITEM");
    }

    @Test
    void addFeedbackItem_withUnknownOrMissingType_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(retro.getId(), "x", "MEH", MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("unknown type 'MEH'");
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(retro.getId(), "x", null, MEMBER, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void addFeedbackItem_withBlankContentOrAuthor_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(retro.getId(), " ", FeedbackType.WENT_WELL, MEMBER, null))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(retro.getId(), "x", FeedbackType.WENT_WELL, " ", null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void addFeedbackItem_toUnknownRetrospective_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.addFeedbackItem(UNKNOWN_ID, "x", FeedbackType.WENT_WELL, MEMBER, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addActionItem_withUnknownPriority_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.addActionItem(retro.getId(), "Fix CI", "URGENT", MEMBER))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("(allowed: [LOW, MEDIUM, HIGH])");
    }

    @Test
    void addActionItem_toClosedRetrospective_throwsException() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.addActionItem(retro.getId(), "Fix CI", null, MEMBER))
                .isExactlyInstanceOf(RetroflowException.class);
    }

    @Test
    void updateFeedbackContent_inOpenRetrospective_updatesContent() {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(retro.getId(), "Old", "WENT_WELL", MEMBER);

        // Act
        FeedbackItem updated = feedbackService.updateFeedbackContent(item.getId(), " New ");

        // Assert
        assertThat(updated.getContent()).isEqualTo("New");
    }

    @Test
    void updateFeedbackContent_inClosedRetrospective_throwsException() {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(retro.getId(), "Old", "WENT_WELL", MEMBER);
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.updateFeedbackContent(item.getId(), "New"))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: cannot modify feedback in a closed retrospective", retro.getId());
    }

    @Test
    void updateFeedbackContent_withUnknownId_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackService.updateFeedbackContent(UNKNOWN_ID, "New"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteFeedbackItem_inOpenRetrospective_removesIt() {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(retro.getId(), "Oops", "WENT_WELL", MEMBER);

        // Act
        feedbackService.deleteFeedbackItem(item.getId());

        // Assert
        assertThat(feedbackItemRepository.existsById(item.getId())).isFalse();
    }

    @Test
    void deleteFeedbackItem_inClosedRetrospective_throwsExceptionAndKeepsItem() {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(retro.getId(), "Keep", "WENT_WELL", MEMBER);
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.deleteFeedbackItem(item.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective %d: cannot delete feedback in a closed retrospective", retro.getId());
        assertThat(feedbackItemRepository.existsById(item.getId())).isTrue();
    }

}

