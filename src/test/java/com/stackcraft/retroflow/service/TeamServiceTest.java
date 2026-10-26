package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import static com.stackcraft.retroflow.TestData.uniqueTeamName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TeamServiceTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final List<String> MINIMUM_VIABLE_TEAM = List.of("Alice"); // 1 member is the minimum

    @Autowired
    private TeamService teamService;

    @Test
    void createTeam_withValidInput_trimsAndPersistsNameAndMembers() {
        // Arrange
        String name = uniqueTeamName();

        // Act
        Team created = teamService.createTeam("  " + name + " ", List.of(" Grace ", "Heidi"));

        // Assert
        Team found = teamService.getTeam(created.getId());
        assertThat(found.getName()).isEqualTo(name);
        assertThat(found.getMembers()).containsExactly("Grace", "Heidi");
    }

    @Test
    void createTeam_withBlankName_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(" ", MINIMUM_VIABLE_TEAM))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: name must not be blank");
    }

    @Test
    void createTeam_withoutMembers_throwsInvalidInput() {
        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), List.of()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: must have at least one member");
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createTeam_withBlankMemberName_throwsInvalidInput() {
        // Arrange
        List<String> membersWithBlank = Arrays.asList("Alice", null);

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), membersWithBlank))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: member name must not be blank");
    }

    @Test
    void createTeam_withDuplicateMemberNamesIgnoringCase_throwsInvalidInput() {
        // Arrange
        List<String> duplicateMembers = List.of("Alice", "ALICE ");

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), duplicateMembers))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: duplicate member name 'ALICE'");
    }

    @Test
    void createTeam_withExistingNameIgnoringCase_throwsConflict() {
        // Arrange
        String name = uniqueTeamName();
        teamService.createTeam(name, MINIMUM_VIABLE_TEAM);

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(name.toUpperCase(), MINIMUM_VIABLE_TEAM))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getTeam_withUnknownId_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> teamService.getTeam(UNKNOWN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Team %d: not found".formatted(UNKNOWN_ID));
    }

}


