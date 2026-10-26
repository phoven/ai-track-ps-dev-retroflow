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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * A retrospective of one team, OPEN or CLOSED. A team has at most one OPEN
 * retrospective, and closing is one-way (enforced by {@code RetrospectiveService}).
 *
 * No setters for {@code status} (use {@link #close()}) or {@code team} (moving a
 * retrospective to another team would bypass the one-open rule): decision D12.
 */
@Entity
public class Retrospective {

    public static final int MAX_TITLE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: responses only need the team id, which Hibernate serves without loading the team
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @NotBlank
    @Size(max = MAX_TITLE_LENGTH)
    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @NotNull
    @Column(name = "retro_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    public Retrospective() {
        // for JPA
    }

    /** All settable fields; a new retrospective always starts OPEN. */
    public Retrospective(Team team, String title, LocalDate date) {
        this.team = team;
        this.title = title;
        this.date = date;
    }

    public boolean isOpen() {
        return status == RetrospectiveStatus.OPEN;
    }

    /** One-way transition; callers must check {@link #isOpen()} first. */
    public void close() {
        this.status = RetrospectiveStatus.CLOSED;
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

    /** Only allowed while OPEN: the caller (service) checks that (D15). */
    public void rename(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    /** Only allowed while OPEN: the caller (service) checks that (D15). */
    public void reschedule(LocalDate date) {
        this.date = date;
    }

    public RetrospectiveStatus getStatus() {
        return status;
    }

}

