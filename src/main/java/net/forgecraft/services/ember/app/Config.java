package net.forgecraft.services.ember.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private DiscordConfig discord = DiscordConfig.create();

    public Config(Path configPath) {
        load(configPath);
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
            var mapper = new ObjectMapper();
            try {
                String defaultConfigString = mapper.writeValueAsString(new Config());
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

    public DiscordConfig getDiscord() {
        return discord;
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
    ) {}
}
