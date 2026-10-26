package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCase(String name);

    /**
     * Loads the team with a row lock held until the transaction ends, so two
     * concurrent "create retrospective" requests for the same team are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Team> findWithLockById(Long id);

}

