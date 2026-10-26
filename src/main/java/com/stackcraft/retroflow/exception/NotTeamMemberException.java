package com.stackcraft.retroflow.exception;

/** The person creating or closing a retrospective is not a member of its team (decision D3). Mapped to 403. */
public class NotTeamMemberException extends RetroflowException {

    public NotTeamMemberException(Long teamId, String person) {
        super("Team", teamId, "'%s' is not a member of this team".formatted(person));
    }

}

