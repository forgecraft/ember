package net.forgecraft.services.ember;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {
    private static final String DEFAULT_CONFIG_PATH = "default-config.json";

    private DiscordConfig discord;

    public Config() {
        load();
    }

    public void load() {
        var mapper = new ObjectMapper();

        //
    }

    public record DiscordConfig(
            String token,
            String guild
    ) {}
}
