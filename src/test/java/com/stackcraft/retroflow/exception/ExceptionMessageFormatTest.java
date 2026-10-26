package com.stackcraft.retroflow.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standards §2.1: every service-layer exception message is {@code [EntityType] [identifier]: [reason]},
 * because the observability platform parses it. Guards that each domain exception produces that
 * format and that none of them can be built from a free-form message.
 */
class ExceptionMessageFormatTest {

    private static final Pattern STANDARD_FORMAT = Pattern.compile("^[A-Z][A-Za-z]* \\S+: \\S.*$");
    private static final long EXISTING_ID = 42L;
    private static final String REASON = "cannot add feedback to a closed retrospective";
    private static final List<Class<? extends RuntimeException>> DOMAIN_EXCEPTIONS = List.of(
            RetroflowException.class, InvalidInputException.class,
            NotTeamMemberException.class, ResourceNotFoundException.class);

    @Test
    void constructor_withEntityTypeIdentifierAndReason_producesStandardFormat() {
        // Arrange
        List<RuntimeException> exceptions = List.of(
                new RetroflowException("Retrospective", EXISTING_ID, REASON),
                new InvalidInputException("Team", EXISTING_ID, "must have at least one member"),
                new NotTeamMemberException(EXISTING_ID, "Zed"),
                new ResourceNotFoundException("ActionItem", EXISTING_ID));

        // Act & Assert
        assertThat(exceptions).extracting(RuntimeException::getMessage)
                .allMatch(message -> STANDARD_FORMAT.matcher(message).matches());
        assertThat(exceptions.get(0)).hasMessage("Retrospective 42: " + REASON);
    }

    @Test
    void constructor_withoutIdentifier_usesNewAsIdentifier() {
        // Act
        RetroflowException exception = new RetroflowException("Team", null, "a team named 'Alpha' already exists");

        // Assert
        assertThat(exception).hasMessage("Team new: a team named 'Alpha' already exists");
    }

    @Test
    void publicConstructors_ofDomainExceptions_neverTakeAFreeFormMessage() {
        // Act
        List<String> freeFormConstructors = DOMAIN_EXCEPTIONS.stream()
                .flatMap(type -> List.of(type.getDeclaredConstructors()).stream())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .filter(ExceptionMessageFormatTest::takesOnlyAMessage)
                .map(Constructor::toString)
                .toList();

        // Assert
        assertThat(freeFormConstructors)
                .as("a (String message) constructor bypasses the §2.1 format")
                .isEmpty();
    }

    private static boolean takesOnlyAMessage(Constructor<?> constructor) {
        return constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0] == String.class;
    }

}

