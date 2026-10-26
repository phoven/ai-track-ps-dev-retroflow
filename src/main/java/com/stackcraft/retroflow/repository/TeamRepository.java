package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Loads the team with a row lock held until the transaction ends. Used to
     * serialize "create retrospective" per team so two concurrent requests
     * can't both pass the "no open retrospective yet" check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Team> findWithLockById(Long id);

}
