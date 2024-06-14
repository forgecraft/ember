package net.forgecraft.services.ember.app.config;

import java.util.Optional;

public record ModrinthConfig(
        Optional<String> accessToken
) {
    public static ModrinthConfig create() {
        return new ModrinthConfig(Optional.empty());
    }
}
