package net.forgecraft.services.ember.mods.downloader;

import net.forgecraft.services.ember.app.mods.parser.ModInfoParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class ModInfoParserTest {
    @Test
    void canParseValidForgeMod() {
        // TODO: Fix this location to something non-absolute
        var pathToMod = Path.of("./downloads/jei-1.19.1-forge-11.2.0.244.jar");
        var parser = new ModInfoParser(pathToMod);

        var info = assertDoesNotThrow(parser::parse);

        // Not all mods will have the same values for both forge and fabric!
        assertEquals("jei", info.id());
        assertEquals("Just Enough Items", info.name());
        assertEquals("11.2.0.244", info.version());
    }

    @Test
    void canParseValidFabricMod() {
        // TODO: Fix this location to something non-absolute
        var pathToMod = Path.of("./downloads/jei-1.19.1-fabric-11.2.0.244.jar");
        var parser = new ModInfoParser(pathToMod);

        var info = assertDoesNotThrow(parser::parse);
        assertEquals("jei", info.id());
        assertEquals("Just Enough Items", info.name());
        assertEquals("11.2.0.244", info.version());
    }
}
