package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.controller.dto.ActionItemDtos.ActionItemResponse;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.CloseRetrospectiveRequest;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.RetrospectiveResponse;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.UpdateRetrospectiveRequest;
import com.stackcraft.retroflow.controller.mapper.ResponseMapper;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.service.FeedbackService;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * POST /api/teams/{teamId}/retrospectives → 201 | 400 | 403 not a member | 404 team | 409 already one OPEN
 * GET  /api/teams/{teamId}/retrospectives → 200 (newest first) | 400 malformed id | 404 team
 * GET  /api/retrospectives/{id}           → 200 | 400 malformed id | 404
 * PUT  /api/retrospectives/{id}/close     → 200 | 400 | 403 not a member | 404 | 409 already CLOSED
 * PATCH /api/retrospectives/{id}          → 200 | 400 empty or invalid body | 403 not a member | 404 | 409 CLOSED (D15)
 * GET  /api/retrospectives/{id}/action-items (optional priority, completed) → 200 | 400 invalid filter value | 404 (D9)
 */
@RestController
@RequestMapping("/api")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;
    private final FeedbackService feedbackService;

    public RetrospectiveController(RetrospectiveService retrospectiveService, FeedbackService feedbackService) {
        this.retrospectiveService = retrospectiveService;
        this.feedbackService = feedbackService;
    }

    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(@PathVariable Long teamId,
                                                                     @Valid @RequestBody CreateRetrospectiveRequest request) {
        RetrospectiveResponse retrospective = ResponseMapper.toResponse(retrospectiveService.createRetrospective(
                teamId, request.title(), request.date(), request.createdBy()));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/retrospectives/{id}").buildAndExpand(retrospective.id()).toUri();
        return ResponseEntity.created(location).body(retrospective);
    }

    @GetMapping("/teams/{teamId}/retrospectives")
    public List<RetrospectiveResponse> listRetrospectives(@PathVariable Long teamId) {
        return ResponseMapper.toRetrospectiveResponses(retrospectiveService.listRetrospectives(teamId));
    }

    @GetMapping("/retrospectives/{id}")
    public RetrospectiveResponse getRetrospective(@PathVariable Long id) {
        return ResponseMapper.toResponse(retrospectiveService.getRetrospective(id));
    }

    @PutMapping("/retrospectives/{id}/close")
    public RetrospectiveResponse closeRetrospective(@PathVariable Long id,
                                                    @Valid @RequestBody CloseRetrospectiveRequest request) {
        return ResponseMapper.toResponse(retrospectiveService.closeRetrospective(id, request.closedBy()));
    }

    @PatchMapping("/retrospectives/{id}")
    public RetrospectiveResponse updateRetrospective(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateRetrospectiveRequest request) {
        return ResponseMapper.toResponse(retrospectiveService.updateRetrospective(
                id, request.title(), request.date(), request.status(), request.updatedBy()));
    }

    @GetMapping("/retrospectives/{id}/action-items")
    public List<ActionItemResponse> listActionItems(@PathVariable Long id,
                                                    @RequestParam(required = false) Priority priority,
                                                    @RequestParam(required = false) Boolean completed) {
        return ResponseMapper.toActionItemResponses(feedbackService.listActionItems(id, priority, completed));
    }

}
