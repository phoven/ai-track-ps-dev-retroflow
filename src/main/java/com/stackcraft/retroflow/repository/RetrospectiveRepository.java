package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    boolean existsByTeamIdAndStatus(Long teamId, RetrospectiveStatus status);

}
