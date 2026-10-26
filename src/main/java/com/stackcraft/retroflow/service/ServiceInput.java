package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotTeamMemberException;

import java.util.Arrays;

/** Input checks shared by the services. Violations are reported as {@link InvalidInputException} (400). */
final class ServiceInput {

    private ServiceInput() {
    }

    /** Returns the trimmed value, or throws if it is null or blank. */
    static String requireText(String value, String entityType, Object identifier, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(entityType, identifier, field + " must not be blank");
        }
        return value.strip();
    }

    /** Throws if the value is null. */
    static void requirePresent(Object value, String entityType, Object identifier, String field) {
        if (value == null) {
            throw new InvalidInputException(entityType, identifier, field + " must not be empty");
        }
    }

    /** Throws {@link NotTeamMemberException} if the (non-blank) person is not a member of the team (D3). */
    static void requireMember(Team team, String person) {
        String name = requireText(person, "Team", team.getId(), "member name");
        if (!team.hasMember(name)) {
            throw new NotTeamMemberException(team.getId(), name);
        }
    }

    /** Case-insensitive enum lookup; throws listing the allowed values. */
    static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String entityType, String field) {
        if (value != null) {
            for (E constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(value.strip())) {
                    return constant;
                }
            }
        }
        throw new InvalidInputException(entityType, null, "unknown %s '%s' (allowed: %s)"
                .formatted(field, value, Arrays.toString(enumType.getEnumConstants())));
    }

}

