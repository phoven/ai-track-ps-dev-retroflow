package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

/**
 * A retrospective belongs to a team and is either OPEN or CLOSED. A team has
 * at most one OPEN retrospective at a time (enforced by
 * {@code RetrospectiveService}); once closed, it can never be reopened.
 */
@Entity
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String title;

    @Column(name = "retro_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    protected Retrospective() {
        // for JPA
    }

    public Retrospective(Team team, String title, LocalDate date) {
        this.team = team;
        this.title = title;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDate() {
        return date;
    }

    public RetrospectiveStatus getStatus() {
        return status;
    }

    public boolean isOpen() {
        return status == RetrospectiveStatus.OPEN;
    }

    /** Closing is one-way; callers must check {@link #isOpen()} first. */
    public void close() {
        this.status = RetrospectiveStatus.CLOSED;
    }

}
