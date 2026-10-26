package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    /**
     * Action items of one retrospective (D9), oldest first. A null filter means "any".
     * One query with optional parameters instead of a derived method per combination.
     */
    @Query("""
            select a from ActionItem a
            where a.retrospective.id = :retrospectiveId
              and (:priority is null or a.priority = :priority)
              and (:completed is null or a.completed = :completed)
            order by a.id""")
    List<ActionItem> findByRetrospectiveFiltered(@Param("retrospectiveId") Long retrospectiveId,
                                                 @Param("priority") Priority priority,
                                                 @Param("completed") Boolean completed);

}

