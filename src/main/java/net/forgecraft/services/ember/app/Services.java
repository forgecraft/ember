package net.forgecraft.services.ember.app;

import net.forgecraft.services.ember.helpers.ArgsParser;

/**
 * Basic services class to use as dependency injection
 */
public class Services {
    private final Config config;

    public Services(String[] args) {
        var appArgs = ArgsParser.parse(args);

        this.config = new Config(appArgs.getOrThrow("config"));
    }

    public Config getConfig() {
        return config;
    }
}
