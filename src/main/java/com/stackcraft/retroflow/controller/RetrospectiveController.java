package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.CloseRetrospectiveRequest;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.RetrospectiveResponse;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * POST /api/teams/{teamId}/retrospectives → 201 | 400 | 403 not a member | 404 team | 409 already one OPEN
 * GET  /api/teams/{teamId}/retrospectives → 200 (newest first) | 400 malformed id | 404 team
 * GET  /api/retrospectives/{id}           → 200 | 400 malformed id | 404
 * PUT  /api/retrospectives/{id}/close     → 200 | 400 | 403 not a member | 404 | 409 already CLOSED
 */
@RestController
@RequestMapping("/api")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(@PathVariable Long teamId,
                                                                     @Valid @RequestBody CreateRetrospectiveRequest request) {
        RetrospectiveResponse retrospective = RetrospectiveResponse.from(retrospectiveService.createRetrospective(
                teamId, request.title(), request.date(), request.createdBy()));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/retrospectives/{id}").buildAndExpand(retrospective.id()).toUri();
        return ResponseEntity.created(location).body(retrospective);
    }

    @GetMapping("/teams/{teamId}/retrospectives")
    public List<RetrospectiveResponse> listRetrospectives(@PathVariable Long teamId) {
        return retrospectiveService.listRetrospectives(teamId).stream().map(RetrospectiveResponse::from).toList();
    }

    @GetMapping("/retrospectives/{id}")
    public RetrospectiveResponse getRetrospective(@PathVariable Long id) {
        return RetrospectiveResponse.from(retrospectiveService.getRetrospective(id));
    }

    @PutMapping("/retrospectives/{id}/close")
    public RetrospectiveResponse closeRetrospective(@PathVariable Long id,
                                                    @Valid @RequestBody CloseRetrospectiveRequest request) {
        return RetrospectiveResponse.from(retrospectiveService.closeRetrospective(id, request.closedBy()));
    }

}

