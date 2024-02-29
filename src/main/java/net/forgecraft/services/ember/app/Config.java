package net.forgecraft.services.ember.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private DiscordConfig discord;

    public Config(String configPath) {
        load(Path.of(configPath));
    }

    // For jackson
    public Config() {}

    public void load(Path path) {
        var configRaw = this.loadFromPath(path);

        var mapper = new ObjectMapper();
        try {
            var appConfig = mapper.readValue(configRaw, Config.class);

            this.discord = appConfig.discord;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadFromPath(Path path) throws RuntimeException {
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
            var defaultConfig = createDefaultConfig();
            var mapper = new ObjectMapper();
            try {
                String defaultConfigString = mapper.writeValueAsString(defaultConfig);
                Files.writeString(path, defaultConfigString);
                return defaultConfigString;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // We exist so just read it
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Config createDefaultConfig() {
        var config = new Config();
        config.discord = new DiscordConfig("YOUR_DISCORD", -1, new long[] {});

        return config;
    }

    public DiscordConfig getDiscord() {
        return discord;
    }

    public record DiscordConfig(
            String token,
            long guild,
            long[] adminRoles
    ) {}

    public record ServerAutomationsConfig(
            String serverPath,
            String slug,
            long serverUpdateChannel
    ) {}
}
