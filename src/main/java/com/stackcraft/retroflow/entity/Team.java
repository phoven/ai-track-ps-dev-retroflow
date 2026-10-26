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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A team: a unique name (D10) and at least one member, stored as names only.
 * Teams can't be changed after creation (D11), so there are no setters (D12).
 */
@Entity
public class Team {

    public static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = MAX_NAME_LENGTH)
    @Column(nullable = false, unique = true, length = MAX_NAME_LENGTH)
    private String name;

    // Small and always needed with the team, so loaded eagerly (no lazy-loading errors outside a transaction).
    // Unidirectional: a team doesn't hold its retrospectives; query them via RetrospectiveRepository.
    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_member", joinColumns = @JoinColumn(name = "team_id"))
    @OrderColumn(name = "position")
    @Column(name = "member_name", nullable = false, length = MAX_NAME_LENGTH)
    private List<@NotBlank @Size(max = MAX_NAME_LENGTH) String> members = new ArrayList<>();

    public Team() {
        // for JPA
    }

    public Team(String name, List<String> members) {
        this.name = name;
        this.members = new ArrayList<>(members);
    }

    /** Case-insensitive membership check (member names are unique case-insensitively, D10). */
    public boolean hasMember(String memberName) {
        return memberName != null && members.stream().anyMatch(member -> member.equalsIgnoreCase(memberName.strip()));
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

