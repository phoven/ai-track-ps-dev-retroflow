package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.controller.dto.TeamDtos.CreateTeamRequest;
import com.stackcraft.retroflow.controller.dto.TeamDtos.TeamResponse;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * POST /api/teams      → 201 | 400 invalid body | 409 name taken
 * GET  /api/teams/{id} → 200 | 400 malformed id | 404
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse team = TeamResponse.from(teamService.createTeam(request.name(), request.members()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(team.id()).toUri();
        return ResponseEntity.created(location).body(team);
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return TeamResponse.from(teamService.getTeam(id));
    }

}

