package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Creating and retrieving teams. Teams can't be changed after creation (D11). */
@Service
@Transactional
public class TeamService {

    private static final String TEAM = "Team";

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Creates a team. Name and member names are trimmed.
     *
     * @param name    team name, unique case-insensitively (D10)
     * @param members member names: at least one, none blank, unique case-insensitively (D10)
     * @return the persisted team
     * @throws InvalidInputException if the name or a member name is blank, there are no members,
     *                               or member names are duplicated
     * @throws RetroflowException    if another team already has this name
     */
    public Team createTeam(String name, List<String> members) {
        String teamName = ServiceInput.requireText(name, TEAM, null, "name");
        List<String> memberNames = validateMembers(members);
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw new RetroflowException(TEAM, null, "a team named '%s' already exists".formatted(teamName));
        }
        return teamRepository.save(new Team(teamName, memberNames));
    }

    /**
     * Returns a team with its members.
     *
     * @param teamId the team id
     * @return the team
     * @throws ResourceNotFoundException if no team has this id
     */
    @Transactional(readOnly = true)
    public Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(TEAM, teamId));
    }

    private static List<String> validateMembers(List<String> members) {
        if (members == null || members.isEmpty()) {
            throw new InvalidInputException(TEAM, null, "must have at least one member");
        }
        List<String> memberNames = members.stream()
                .map(member -> ServiceInput.requireText(member, TEAM, null, "member name"))
                .toList();
        Set<String> seen = new HashSet<>();
        for (String member : memberNames) {
            if (!seen.add(member.toLowerCase(Locale.ROOT))) {
                throw new InvalidInputException(TEAM, null, "duplicate member name '%s'".formatted(member));
            }
        }
        return memberNames;
    }

}

