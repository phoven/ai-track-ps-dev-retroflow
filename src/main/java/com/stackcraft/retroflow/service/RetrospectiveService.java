package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotTeamMemberException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Retrospective lifecycle (one OPEN per team, closing is one-way), plus the feedback
 * operations the vendor test calls on this service, delegated to {@link FeedbackService}.
 */
@Service
@Transactional
public class RetrospectiveService {

    private static final String TEAM = "Team";
    private static final String RETROSPECTIVE = "Retrospective";

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackService feedbackService;

    public RetrospectiveService(TeamRepository teamRepository,
                                RetrospectiveRepository retrospectiveRepository,
                                FeedbackService feedbackService) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackService = feedbackService;
    }

    /**
     * Opens a retrospective on behalf of a team member (D3) for the given date (D4).
     * @param teamId    the team
     * @param title     retrospective title
     * @param date      date of the retrospective, any date
     * @param createdBy name of the team member opening it
     * @return the new OPEN retrospective
     * @throws ResourceNotFoundException if the team doesn't exist
     * @throws InvalidInputException     if title, date or createdBy is missing
     * @throws NotTeamMemberException    if createdBy is not a member of the team
     * @throws RetroflowException        if the team already has an OPEN retrospective
     */
    public Retrospective createRetrospective(Long teamId, String title, LocalDate date, String createdBy) {
        Team team = lockTeam(teamId);
        ServiceInput.requireMember(team, createdBy);
        return open(team, title, date);
    }

    /**
     * Vendor-test overload (D13): date = today, no member check. The API never calls this.
     * @param teamId the team
     * @param title  retrospective title
     * @return the new OPEN retrospective
     * @throws ResourceNotFoundException if the team doesn't exist
     * @throws RetroflowException        if the title is blank or the team already has an OPEN retrospective
     */
    public Retrospective createRetrospective(Long teamId, String title) {
        return open(lockTeam(teamId), title, LocalDate.now());
    }

    /**
     * Returns a retrospective.
     * @param retrospectiveId the retrospective
     * @return the retrospective
     * @throws ResourceNotFoundException if it doesn't exist
     */
    @Transactional(readOnly = true)
    public Retrospective getRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new ResourceNotFoundException(RETROSPECTIVE, retrospectiveId));
    }

    /**
     * Lists a team's retrospectives.
     * @param teamId the team
     * @return the team's retrospectives, newest first
     * @throws ResourceNotFoundException if the team doesn't exist
     */
    @Transactional(readOnly = true)
    public List<Retrospective> listRetrospectives(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException(TEAM, teamId);
        }
        return retrospectiveRepository.findByTeamIdOrderByDateDescIdDesc(teamId);
    }

    /**
     * Closes a retrospective on behalf of a team member (D3). Closing is one-way.
     * @param retrospectiveId the retrospective
     * @param closedBy        name of the team member closing it
     * @return the CLOSED retrospective
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws InvalidInputException     if closedBy is missing
     * @throws NotTeamMemberException    if closedBy is not a member of the team
     * @throws RetroflowException        if it is already closed (D7)
     */
    public Retrospective closeRetrospective(Long retrospectiveId, String closedBy) {
        Retrospective retrospective = getRetrospective(retrospectiveId);
        ServiceInput.requireMember(retrospective.getTeam(), closedBy);
        return close(retrospective);
    }

    /**
     * Vendor-test overload (D13): no member check. The API never calls this.
     * @param retrospectiveId the retrospective
     * @return the CLOSED retrospective
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws RetroflowException        if it is already closed (D7)
     */
    public Retrospective closeRetrospective(Long retrospectiveId) {
        return close(getRetrospective(retrospectiveId));
    }

    /**
     * Vendor-test entry point; see {@link FeedbackService#addFeedbackItem}.
     * @param retrospectiveId the retrospective
     * @param content         feedback text
     * @param type            feedback type name, case-insensitive
     * @param author          name of the author
     * @return the new feedback item
     * @throws InvalidInputException     if a value is blank or the type is unknown
     * @throws ResourceNotFoundException if the retrospective doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String author) {
        FeedbackType feedbackType = ServiceInput.parseEnum(FeedbackType.class, type, "FeedbackItem", "type");
        return feedbackService.addFeedbackItem(retrospectiveId, content, feedbackType, author, null);
    }

    /**
     * Vendor-test entry point; see {@link FeedbackService#addActionItem}.
     * @param retrospectiveId the retrospective
     * @param content         action item text
     * @param priority        priority name, case-insensitive; null means MEDIUM (D6)
     * @param author          name of the author
     * @return the new action item
     * @throws InvalidInputException     if a value is blank or the priority is unknown
     * @throws ResourceNotFoundException if the retrospective doesn't exist
     * @throws RetroflowException        if the retrospective is closed
     */
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String author) {
        Priority actionPriority = priority == null ? null
                : ServiceInput.parseEnum(Priority.class, priority, "ActionItem", "priority");
        return feedbackService.addActionItem(retrospectiveId, content, author, actionPriority);
    }

    /**
     * Vendor-test entry point; see {@link FeedbackService#completeActionItem}.
     * @param actionItemId the action item
     * @return the completed action item
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws RetroflowException        if it is already completed
     */
    public ActionItem completeActionItem(Long actionItemId) {
        return feedbackService.completeActionItem(actionItemId);
    }

    /**
     * Vendor-test entry point; see {@link FeedbackService#uncompleteActionItem}.
     * @param actionItemId the action item
     * @return the unchanged action item
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws RetroflowException        if it is completed
     */
    public ActionItem uncompleteActionItem(Long actionItemId) {
        return feedbackService.uncompleteActionItem(actionItemId);
    }

    /**
     * Changes the title and/or date of an OPEN retrospective on behalf of a team member (D15).
     * A CLOSED retrospective is frozen and can't be reopened (brief). The status can't be changed
     * here: closing goes through {@link #closeRetrospective(Long, String)}.
     *
     * @param retrospectiveId the retrospective
     * @param title           new title, or null to keep it
     * @param date            new date, or null to keep it
     * @param status          null, or the current status; OPEN on a CLOSED retrospective is a reopen attempt
     * @param updatedBy       name of the team member making the change
     * @return the updated retrospective
     * @throws InvalidInputException     if nothing is changed, the title is blank, updatedBy is missing,
     *                                   or status CLOSED is sent for an OPEN retrospective
     * @throws ResourceNotFoundException if it doesn't exist
     * @throws NotTeamMemberException    if updatedBy is not a member of the team
     * @throws RetroflowException        if it is CLOSED (reopen attempt or any other change)
     */
    public Retrospective updateRetrospective(Long retrospectiveId, String title, LocalDate date,
                                             RetrospectiveStatus status, String updatedBy) {
        if (title == null && date == null && status == null) {
            throw new InvalidInputException(RETROSPECTIVE, retrospectiveId, "nothing to update: send title, date and/or status");
        }
        Retrospective retrospective = getRetrospective(retrospectiveId);
        ServiceInput.requireMember(retrospective.getTeam(), updatedBy);
        if (!retrospective.isOpen()) {
            throw new RetroflowException(RETROSPECTIVE, retrospectiveId, status == RetrospectiveStatus.OPEN
                    ? "closed, cannot be reopened" : "closed, cannot be modified");
        }
        if (status == RetrospectiveStatus.CLOSED) {
            throw new InvalidInputException(RETROSPECTIVE, retrospectiveId,
                    "status can't be changed here: use the close operation, which records who closed it");
        }
        if (title != null) {
            retrospective.rename(ServiceInput.requireText(title, RETROSPECTIVE, retrospectiveId, "title"));
        }
        if (date != null) {
            retrospective.reschedule(date);
        }
        return retrospective;
    }

    private Team lockTeam(Long teamId) {
        return teamRepository.findWithLockById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(TEAM, teamId));
    }

    private Retrospective open(Team team, String title, LocalDate date) {
        String retroTitle = ServiceInput.requireText(title, RETROSPECTIVE, null, "title");
        ServiceInput.requirePresent(date, RETROSPECTIVE, null, "date");
        var openRetro = retrospectiveRepository.findFirstByTeamIdAndStatus(team.getId(), RetrospectiveStatus.OPEN);
        if (openRetro.isPresent()) {
            throw new RetroflowException(TEAM, team.getId(), "already has an open retrospective (Retrospective %d)"
                    .formatted(openRetro.get().getId()));
        }
        return retrospectiveRepository.save(new Retrospective(team, retroTitle, date));
    }

    private Retrospective close(Retrospective retrospective) {
        if (!retrospective.isOpen()) {
            throw new RetroflowException(RETROSPECTIVE, retrospective.getId(), "already closed, cannot be closed again");
        }
        retrospective.close();
        return retrospective;
    }


}

