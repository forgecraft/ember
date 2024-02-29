package net.forgecraft.services.ember.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsParserTest {
    @Test
    public void parsesFlagWithoutValue() {
        var parser = ArgsParser.parse(new String[] { "--debug" });
        assertNull(parser.get("debug"));
    }

    @Test
    public void parsesFlagWithValue() {
        var parser = ArgsParser.parse(new String[] { "--test=123" });
        assertEquals("123", parser.get("test"));
    }

    @Test
    public void parsesMultipleFlags() {
        var parser = ArgsParser.parse(new String[] { "--debug", "--test=123" });
        assertNull(parser.get("debug"));
        assertEquals("123", parser.get("test"));
    }

    @Test
    public void ignoresNonFlagArguments() {
        var parser = ArgsParser.parse(new String[] { "ignoreme", "--debug" });
        assertNull(parser.get("debug"));
        assertNull(parser.get("ignoreme"));
    }

    @Test
    public void throwsExceptionForMissingRequiredFlag() {
        var parser = ArgsParser.parse(new String[] { "--debug" });
        assertThrows(IllegalArgumentException.class, () -> parser.getOrThrow("test"));
    }
}
