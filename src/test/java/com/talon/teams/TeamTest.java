package com.talon.teams;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TeamTest {
    @Test
    public void parsesTeamNamesWithoutCaseSensitivity() {
        assertEquals(Team.RED, Team.fromString("red"));
        assertEquals(Team.BLUE, Team.fromString("BLUE"));
        assertEquals(Team.GREEN, Team.fromString("Green"));
        assertEquals(Team.YELLOW, Team.fromString("yellow"));
    }

    @Test
    public void parsesNamesWithSurroundingWhitespace() {
        assertEquals(Team.RED, Team.fromString("  Red  "));
    }

    @Test
    public void rejectsUnknownOrMissingNames() {
        assertNull(Team.fromString("purple"));
        assertNull(Team.fromString(""));
        assertNull(Team.fromString(null));
    }
}
