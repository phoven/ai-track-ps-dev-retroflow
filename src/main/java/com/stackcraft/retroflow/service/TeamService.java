package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team createTeam(String name, List<String> members) {
        if (name == null || name.isBlank()) {
            throw new RetroflowException("Team name must not be blank");
        }
        if (members == null || members.isEmpty()) {
            throw new RetroflowException("A team must have at least one member");
        }
        if (members.stream().anyMatch(member -> member == null || member.isBlank())) {
            throw new RetroflowException("Team member names must not be blank");
        }
        List<String> memberNames = members.stream().map(String::strip).toList();
        return teamRepository.save(new Team(name.strip(), memberNames));
    }

    @Transactional(readOnly = true)
    public Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    }

}

