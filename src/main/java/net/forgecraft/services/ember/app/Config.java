package net.forgecraft.services.ember.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Config {

    private static final ObjectMapper JACKSON_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final DiscordConfig discord = DiscordConfig.create();
    private final ModrinthConfig modrinth = ModrinthConfig.create();

    // For jackson
    public Config() {
    }

    public static Config load(Path path) {
        try (var stream = loadFromPath(path)) {
            return JACKSON_MAPPER.readValue(stream, Config.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config file", e);
        }
    }

    private static InputStream loadFromPath(Path path) throws IOException {
        if (Files.notExists(path)) {
            // Create it
            var parent = path.getParent();
            if (parent != null) {
                try {
                    Files.createDirectories(parent);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create config file", e);
                }
            }

            // Write the default
            try (var writer = Files.newBufferedWriter(path)) {
                JACKSON_MAPPER.writeValue(writer, new Config());
            }
        }

        // We exist so just read it
        return Files.newInputStream(path);
    }

    public DiscordConfig getDiscord() {
        return discord;
    }

    public ModrinthConfig getModrinth() {
        return modrinth;
    }

    public record DiscordConfig(
            String token,
            long guild,
            long[] adminRoles,
            long uploadChannel
    ) {
        public static DiscordConfig create() {
            return new DiscordConfig("YOUR_DISCORD_TOKEN", -1, new long[0], -1);
        }
    }

    public record ServerAutomationsConfig(
            String serverPath,
            String slug,
            long serverUpdateChannel
    ) {
    }

    public record ModrinthConfig(
            Optional<String> accessToken
    ) {
        public static ModrinthConfig create() {
            return new ModrinthConfig(Optional.empty());
        }
    }
}
