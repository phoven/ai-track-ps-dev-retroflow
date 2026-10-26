package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Detailed service-layer tests: the happy paths, the exact reason each
 * business rule is rejected, input validation, and unknown ids.
 * The vendor's {@code RetroflowBusinessRulesTest} only checks the exception
 * type; these tests check the message too, so a rule can't pass by failing
 * for the wrong reason.
 */
@SpringBootTest
class RetrospectiveServiceTest {

    private static final long UNKNOWN_ID = 999_999L;

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    private Team team;
    private Retrospective retro;

    @BeforeEach
    void setUp() {
        team = teamService.createTeam("  Team Delta ", List.of(" Grace ", "Heidi"));
        retro = retrospectiveService.createRetrospective(team.getId(), " Sprint 7 Retro ");
    }

    @Nested
    class Teams {

        @Test
        void createTeam_trimsAndPersistsNameAndMembers() {
            Team found = teamService.getTeam(team.getId());

            assertThat(found.getName()).isEqualTo("Team Delta");
            assertThat(found.getMembers()).containsExactly("Grace", "Heidi");
        }

        @Test
        void createTeam_requiresName() {
            assertThatThrownBy(() -> teamService.createTeam(" ", List.of("Alice")))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("name must not be blank");
            assertThatThrownBy(() -> teamService.createTeam(null, List.of("Alice")))
                    .isInstanceOf(RetroflowException.class);
        }

        @Test
        void createTeam_requiresAtLeastOneMember() {
            assertThatThrownBy(() -> teamService.createTeam("Team", List.of()))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("at least one member");
            assertThatThrownBy(() -> teamService.createTeam("Team", null))
                    .isInstanceOf(RetroflowException.class);
        }

        @Test
        void createTeam_rejectsBlankMemberNames() {
            assertThatThrownBy(() -> teamService.createTeam("Team", Arrays.asList("Alice", null)))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("member names must not be blank");
            assertThatThrownBy(() -> teamService.createTeam("Team", List.of("Alice", " ")))
                    .isInstanceOf(RetroflowException.class);
        }

        @Test
        void getTeam_unknownId() {
            assertThatThrownBy(() -> teamService.getTeam(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Team 999999 not found");
        }
    }

    @Nested
    class Retrospectives {

        @Test
        void createRetrospective_isOpenWithTodaysDate() {
            Retrospective found = retrospectiveService.getRetrospective(retro.getId());

            assertThat(found.getTitle()).isEqualTo("Sprint 7 Retro");
            assertThat(found.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
            assertThat(found.getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        void secondOpenRetrospective_isRejectedWithReason() {
            assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Another"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("already has an open retrospective");
        }

        @Test
        void newRetrospective_isAllowedAfterClosingThePreviousOne() {
            retrospectiveService.closeRetrospective(retro.getId());

            Retrospective next = retrospectiveService.createRetrospective(team.getId(), "Sprint 8 Retro");

            assertThat(next.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        }

        @Test
        void otherTeams_canHaveTheirOwnOpenRetrospective() {
            Team other = teamService.createTeam("Team Epsilon", List.of("Ivan"));

            Retrospective otherRetro = retrospectiveService.createRetrospective(other.getId(), "Sprint 1");

            assertThat(otherRetro.isOpen()).isTrue();
        }

        @Test
        void createRetrospective_requiresTitleAndExistingTeam() {
            assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), " "))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("title must not be blank");
            assertThatThrownBy(() -> retrospectiveService.createRetrospective(UNKNOWN_ID, "Retro"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void closedRetrospective_cannotBeClosedAgain() {
            Retrospective closed = retrospectiveService.closeRetrospective(retro.getId());
            assertThat(closed.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);

            assertThatThrownBy(() -> retrospectiveService.closeRetrospective(retro.getId()))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("is closed");
        }

        @Test
        void unknownRetrospective() {
            assertThatThrownBy(() -> retrospectiveService.closeRetrospective(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Retrospective 999999 not found");
        }
    }

    @Nested
    class Feedback {

        @Test
        void addFeedbackItem_toOpenRetrospective() {
            FeedbackItem item = retrospectiveService.addFeedbackItem(
                    retro.getId(), " Pairing helped ", "went_well", " Grace ");

            assertThat(item.getId()).isNotNull();
            assertThat(item.getContent()).isEqualTo("Pairing helped");
            assertThat(item.getType()).isEqualTo(FeedbackType.WENT_WELL);
            assertThat(item.getAuthor()).isEqualTo("Grace");
            assertThat(item.getRetrospective().getId()).isEqualTo(retro.getId());
        }

        @Test
        void addFeedbackItem_toClosedRetrospective_isRejectedWithReason() {
            retrospectiveService.closeRetrospective(retro.getId());

            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                    retro.getId(), "Too late", "NEEDS_IMPROVEMENT", "Grace"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("is closed; cannot add feedback");
        }

        @Test
        void addFeedbackItem_rejectsUnknownType() {
            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(retro.getId(), "x", "MEH", "Grace"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("Unknown feedback type 'MEH'");
            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(retro.getId(), "x", null, "Grace"))
                    .isInstanceOf(RetroflowException.class);
        }

        @Test
        void addFeedbackItem_rejectsActionItemType() {
            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                    retro.getId(), "Do it", "ACTION_ITEM", "Grace"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("need a priority");
        }

        @Test
        void addFeedbackItem_requiresContentAndAuthor() {
            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(retro.getId(), " ", "WENT_WELL", "Grace"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("content must not be blank");
            assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(retro.getId(), "x", "WENT_WELL", null))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("Author must not be blank");
        }
    }

    @Nested
    class ActionItems {

        @Test
        void addActionItem_isIncompleteWithPriority() {
            ActionItem item = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "medium", "Heidi");

            assertThat(item.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
            assertThat(item.getPriority()).isEqualTo(Priority.MEDIUM);
            assertThat(item.isCompleted()).isFalse();
        }

        @Test
        void addActionItem_toClosedRetrospective_isRejected() {
            retrospectiveService.closeRetrospective(retro.getId());

            assertThatThrownBy(() -> retrospectiveService.addActionItem(retro.getId(), "Fix CI", "LOW", "Heidi"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("cannot add action items");
        }

        @Test
        void addActionItem_rejectsUnknownPriority() {
            assertThatThrownBy(() -> retrospectiveService.addActionItem(retro.getId(), "Fix CI", "URGENT", "Heidi"))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("Unknown priority 'URGENT'")
                    .hasMessageContaining("[LOW, MEDIUM, HIGH]");
        }

        @Test
        void completeActionItem_marksItCompleted() {
            ActionItem item = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", "Heidi");

            ActionItem completed = retrospectiveService.completeActionItem(item.getId());

            assertThat(completed.isCompleted()).isTrue();
        }

        @Test
        void uncompleteActionItem_ofCompletedItem_isRejectedWithReason() {
            ActionItem item = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", "Heidi");
            retrospectiveService.completeActionItem(item.getId());

            assertThatThrownBy(() -> retrospectiveService.uncompleteActionItem(item.getId()))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("cannot be marked incomplete again");
        }

        @Test
        void uncompleteActionItem_ofIncompleteItem_isANoOp() {
            ActionItem item = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", "Heidi");

            ActionItem result = retrospectiveService.uncompleteActionItem(item.getId());

            assertThat(result.isCompleted()).isFalse();
        }

        @Test
        void completeActionItem_ofClosedRetrospective_isRejected() {
            ActionItem item = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", "Heidi");
            retrospectiveService.closeRetrospective(retro.getId());

            assertThatThrownBy(() -> retrospectiveService.completeActionItem(item.getId()))
                    .isInstanceOf(RetroflowException.class)
                    .hasMessageContaining("cannot complete its action items");
        }

        @Test
        void unknownActionItem() {
            assertThatThrownBy(() -> retrospectiveService.completeActionItem(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Action item 999999 not found");
        }

        @Test
        void plainFeedbackItem_isNotAnActionItem() {
            FeedbackItem feedback = retrospectiveService.addFeedbackItem(retro.getId(), "Nice", "WENT_WELL", "Grace");

            assertThatThrownBy(() -> retrospectiveService.completeActionItem(feedback.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

}

