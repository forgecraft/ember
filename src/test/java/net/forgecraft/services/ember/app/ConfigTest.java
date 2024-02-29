package net.forgecraft.services.ember.app;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    public void configLoadAndWritesDefault() {
        var config = new Config("tests/config.json");

        assertNotNull(config);
        assertNotNull(config.getDiscord());

        assertEquals("YOUR_DISCORD", config.getDiscord().token());
    }

    @Test
    public void readsConfigCorrectly() {
        // Write the default config
        new Config("tests/config.json");

        // Read the config
        var config = new Config("tests/config.json");

        assertNotNull(config);
        assertNotNull(config.getDiscord());

        assertEquals("YOUR_DISCORD", config.getDiscord().token());
    }

    @BeforeAll
    public static void setup() throws IOException {
        // Setup the test folder
        var testFolder = Path.of("./tests");
        FileUtils.createParentDirectories(testFolder.toFile());
    }

    @AfterAll
    public static void cleanup() throws IOException {
        // Cleanup the test folder
        var testFolder = Path.of("./tests");
        FileUtils.deleteDirectory(testFolder.toFile());
    }
}
