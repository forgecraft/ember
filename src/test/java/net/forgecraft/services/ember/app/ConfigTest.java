package net.forgecraft.services.ember.app;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    public void configWritesDefault() {
        var configPath = Path.of("tests/config.json");

        Config.load(configPath);

        assertTrue(Files.exists(configPath));
    }

    @Test
    public void defaultConfigParsesCorrectly() {
        var configPath = Path.of("tests/config.json");
        // Write the default config
        Config.load(configPath);

        // Read the config
        var config = Config.load(configPath);

        assertNotNull(config);
        assertNotNull(config.getDiscord());
        assertNotNull(config.getModrinth());

        assertEquals(Config.DiscordConfig.create(), config.getDiscord());
        assertEquals(Config.ModrinthConfig.create(), config.getModrinth());
    }

    @BeforeAll
    public static void setup() throws IOException {
        // Setup the test folder
        var testFolder = Path.of("tests");
        Files.createDirectories(testFolder);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        // Cleanup the test folder
        var testFolder = Path.of("tests");
        FileUtils.deleteDirectory(testFolder.toFile());
    }
}
