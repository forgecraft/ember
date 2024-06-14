package net.forgecraft.services.ember.mods.downloader;

import com.google.common.collect.ImmutableMap;
import net.forgecraft.services.ember.app.mods.parser.ModInfoParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ModInfoParserTest {

    @BeforeAll
    public static void setup() throws Exception {

        Files.createDirectories(Path.of("downloads"));

        var files = ImmutableMap.<String, String>builder()
                .put("https://maven.blamejared.com/mezz/jei/jei-1.19.2-forge/11.2.0.246/jei-1.19.2-forge-11.2.0.246.jar", "downloads/jei-1.19.2-forge-11.2.0.246.jar")
                .put("https://maven.blamejared.com/mezz/jei/jei-1.19.2-fabric/11.2.0.246/jei-1.19.2-fabric-11.2.0.246.jar", "downloads/jei-1.19.2-fabric-11.2.0.246.jar")
                .build();

        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            files.forEach((url, path) -> {
                var request = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).build();
                try {
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofFile(Path.of(path)));
                } catch (IOException | InterruptedException e) {
                    throw new IllegalStateException("Failed to download file", e);
                }
            });
        }
    }

    @Test
    void canParseValidForgeMod() {
        var pathToMod = Path.of("downloads/jei-1.19.2-forge-11.2.0.246.jar");
        var info = assertDoesNotThrow(() -> ModInfoParser.parse(pathToMod));

        assertFalse(info.isEmpty(), "No mod info found");
        var first = info.getFirst();

        assertEquals("jei", first.id());
        assertEquals("Just Enough Items", first.name());
        assertEquals("11.2.0.246", first.version());
    }

    @Test
    void canParseValidFabricMod() {
        var pathToMod = Path.of("downloads/jei-1.19.2-fabric-11.2.0.246.jar");
        var info = assertDoesNotThrow(() -> ModInfoParser.parse(pathToMod));

        assertFalse(info.isEmpty(), "No mod info found");
        var first = info.getFirst();

        assertEquals("jei", first.id());
        assertEquals("Just Enough Items", first.name());
        assertEquals("11.2.0.246", first.version());
    }
}
