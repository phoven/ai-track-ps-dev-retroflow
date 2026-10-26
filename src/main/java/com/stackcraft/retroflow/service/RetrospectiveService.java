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
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Retrospective lifecycle and its feedback/action items. All business rules
 * are enforced here and reported as {@link RetroflowException}; unknown ids
 * are reported as {@link ResourceNotFoundException}.
 */
@Service
@Transactional
public class RetrospectiveService {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public RetrospectiveService(TeamRepository teamRepository,
                                RetrospectiveRepository retrospectiveRepository,
                                FeedbackItemRepository feedbackItemRepository,
                                ActionItemRepository actionItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    public Retrospective createRetrospective(Long teamId, String title) {
        requireText(title, "Retrospective title");
        // Lock the team so concurrent requests can't both create an open retrospective
        Team team = teamRepository.findWithLockById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        if (retrospectiveRepository.existsByTeamIdAndStatus(teamId, RetrospectiveStatus.OPEN)) {
            throw new RetroflowException(
                    "Team %d already has an open retrospective; close it before starting a new one".formatted(teamId));
        }
        return retrospectiveRepository.save(new Retrospective(team, title.strip(), LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public Retrospective getRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective", retrospectiveId));
    }

    public Retrospective closeRetrospective(Long retrospectiveId) {
        Retrospective retrospective = getRetrospective(retrospectiveId);
        requireOpen(retrospective, "close it again");
        retrospective.close();
        return retrospective;
    }

    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String author) {
        FeedbackType feedbackType = parseEnum(FeedbackType.class, type, "feedback type");
        if (feedbackType == FeedbackType.ACTION_ITEM) {
            throw new RetroflowException("Action items need a priority; add them as an action item instead");
        }
        requireText(content, "Feedback content");
        requireText(author, "Author");
        Retrospective retrospective = getRetrospective(retrospectiveId);
        requireOpen(retrospective, "add feedback");
        return feedbackItemRepository.save(
                new FeedbackItem(retrospective, content.strip(), feedbackType, author.strip()));
    }

    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String author) {
        Priority actionPriority = parseEnum(Priority.class, priority, "priority");
        requireText(content, "Action item content");
        requireText(author, "Author");
        Retrospective retrospective = getRetrospective(retrospectiveId);
        requireOpen(retrospective, "add action items");
        return actionItemRepository.save(
                new ActionItem(retrospective, content.strip(), actionPriority, author.strip()));
    }

    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItem(actionItemId);
        // Spec: items of a CLOSED retrospective can never be modified
        requireOpen(actionItem.getRetrospective(), "complete its action items");
        actionItem.complete();
        return actionItem;
    }

    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItem(actionItemId);
        if (actionItem.isCompleted()) {
            throw new RetroflowException(
                    "Action item %d is completed and cannot be marked incomplete again".formatted(actionItemId));
        }
        // Already incomplete: nothing to change
        return actionItem;
    }

    private ActionItem getActionItem(Long actionItemId) {
        return actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Action item", actionItemId));
    }

    private static void requireOpen(Retrospective retrospective, String attemptedAction) {
        if (!retrospective.isOpen()) {
            throw new RetroflowException("Retrospective %d is closed; cannot %s"
                    .formatted(retrospective.getId(), attemptedAction));
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RetroflowException(field + " must not be blank");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String label) {
        if (value != null) {
            for (E constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(value.strip())) {
                    return constant;
                }
            }
        }
        throw new RetroflowException("Unknown %s '%s'; allowed values: %s"
                .formatted(label, value, Arrays.toString(enumType.getEnumConstants())));
    }

}

