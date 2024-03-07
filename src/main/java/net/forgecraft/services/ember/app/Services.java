package net.forgecraft.services.ember.app;

import java.nio.file.Path;

/**
 * Basic services class to use as dependency injection
 */
public class Services {
    private final Config config;

    public Services(Path configPath) {
        this.config = Config.load(configPath);
    }

    public Config getConfig() {
        return config;
    }
}
