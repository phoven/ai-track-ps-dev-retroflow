package com.stackcraft.retroflow.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A team with a name and at least one member (member names only, no
 * authentication). Validation is enforced by {@code TeamService}.
 */
@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Small, always needed together with the team: load eagerly so a returned Team is usable
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_member", joinColumns = @JoinColumn(name = "team_id"))
    @OrderColumn(name = "position")
    @Column(name = "member_name", nullable = false)
    private List<String> members = new ArrayList<>();

    protected Team() {
        // for JPA
    }

    public Team(String name, List<String> members) {
        this.name = name;
        this.members = new ArrayList<>(members);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

}
