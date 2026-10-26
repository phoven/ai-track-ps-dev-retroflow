package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feedback and action items. Items can only be added, edited or deleted while
 * their retrospective is OPEN (D5). Completing an action item is the one change
 * allowed after close (D1), and it is one-way.
 */
@Service
@Transactional
public class FeedbackService {

    private static final String RETROSPECTIVE = "Retrospective";
    private static final String FEEDBACK_ITEM = "FeedbackItem";
    private static final String ACTION_ITEM = "ActionItem";

    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public FeedbackService(RetrospectiveRepository retrospectiveRepository,
                           FeedbackItemRepository feedbackItemRepository,
                           ActionItemRepository actionItemRepository) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    /**
     * Adds feedback to an OPEN retrospective. Type ACTION_ITEM creates an {@link ActionItem}.
     *
     * @param retrospectiveId the retrospective
     * @param content         feedback text
     * @param type            feedback type
     * @param author          name of the author (not checked against the team, D2)
     * @param priority        only for ACTION_ITEM; null means MEDIUM (D6)
     * @return the new feedback item
     * @throws InvalidInputException     if a value is missing, or a priority is given for another type
     * @throws ResourceNotFoundException if the retrospective doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, FeedbackType type,
                                        String author, Priority priority) {
        if (type == FeedbackType.ACTION_ITEM) {
            return addActionItem(retrospectiveId, content, author, priority);
        }
        if (type == null || priority != null) {
            throw new InvalidInputException(FEEDBACK_ITEM, null, type == null
                    ? "type must not be empty" : "priority is only allowed for type ACTION_ITEM");
        }
        String text = ServiceInput.requireText(content, FEEDBACK_ITEM, null, "content");
        String authorName = ServiceInput.requireText(author, FEEDBACK_ITEM, null, "author");
        Retrospective retrospective = openRetrospective(retrospectiveId, "cannot add feedback to a closed retrospective");
        return feedbackItemRepository.save(new FeedbackItem(retrospective, text, type, authorName));
    }

    /**
     * Adds an action item to an OPEN retrospective.
     *
     * @param retrospectiveId the retrospective
     * @param content         action item text
     * @param author          name of the author
     * @param priority        priority; null means MEDIUM (D6)
     * @return the new, not completed action item
     * @throws InvalidInputException     if content or author is blank
     * @throws ResourceNotFoundException if the retrospective doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public ActionItem addActionItem(Long retrospectiveId, String content, String author, Priority priority) {
        String text = ServiceInput.requireText(content, ACTION_ITEM, null, "content");
        String authorName = ServiceInput.requireText(author, ACTION_ITEM, null, "author");
        Retrospective retrospective = openRetrospective(retrospectiveId, "cannot add action items to a closed retrospective");
        return actionItemRepository.save(new ActionItem(retrospective, text, authorName, priority));
    }

    /**
     * Changes the content of a feedback item (or action item) while its retrospective is OPEN.
     *
     * @param feedbackItemId the item
     * @param content        new text
     * @return the updated item
     * @throws InvalidInputException     if content is blank
     * @throws ResourceNotFoundException if the item doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public FeedbackItem updateFeedbackContent(Long feedbackItemId, String content) {
        String text = ServiceInput.requireText(content, FEEDBACK_ITEM, feedbackItemId, "content");
        FeedbackItem item = getFeedbackItem(feedbackItemId);
        requireOpen(item.getRetrospective(), "cannot modify feedback in a closed retrospective");
        item.setContent(text);
        return item;
    }

    /**
     * Deletes a feedback item (or action item) while its retrospective is OPEN.
     *
     * @param feedbackItemId the item
     * @throws ResourceNotFoundException if the item doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public void deleteFeedbackItem(Long feedbackItemId) {
        FeedbackItem item = getFeedbackItem(feedbackItemId);
        requireOpen(item.getRetrospective(), "cannot delete feedback in a closed retrospective");
        feedbackItemRepository.delete(item);
    }

    /**
     * Marks an action item completed. Also allowed after the retrospective is closed (D1).
     *
     * @param actionItemId the action item
     * @return the completed action item
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws RetroflowException        if it is already completed (D7)
     */
    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItem(actionItemId);
        if (actionItem.isCompleted()) {
            throw new RetroflowException(ACTION_ITEM, actionItemId, "already completed");
        }
        actionItem.complete();
        return actionItem;
    }

    /**
     * Rejects un-completing a completed action item; a no-op for an incomplete one (D8).
     *
     * @param actionItemId the action item
     * @return the unchanged action item
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws RetroflowException        if it is completed
     */
    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItem(actionItemId);
        if (actionItem.isCompleted()) {
            throw new RetroflowException(ACTION_ITEM, actionItemId, "already completed, cannot be undone");
        }
        return actionItem;
    }

    private Retrospective openRetrospective(Long retrospectiveId, String reasonIfClosed) {
        Retrospective retrospective = retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new ResourceNotFoundException(RETROSPECTIVE, retrospectiveId));
        requireOpen(retrospective, reasonIfClosed);
        return retrospective;
    }

    private FeedbackItem getFeedbackItem(Long feedbackItemId) {
        return feedbackItemRepository.findById(feedbackItemId)
                .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_ITEM, feedbackItemId));
    }

    private ActionItem getActionItem(Long actionItemId) {
        return actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResourceNotFoundException(ACTION_ITEM, actionItemId));
    }

    private static void requireOpen(Retrospective retrospective, String reason) {
        if (!retrospective.isOpen()) {
            throw new RetroflowException(RETROSPECTIVE, retrospective.getId(), reason);
        }
    }

}

