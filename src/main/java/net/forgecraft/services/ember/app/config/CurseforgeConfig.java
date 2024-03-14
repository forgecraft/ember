package net.forgecraft.services.ember.app.config;

import java.util.Optional;

public record CurseforgeConfig(
        Optional<String> accessToken
) {
    public static CurseforgeConfig create() {
        return new CurseforgeConfig(Optional.empty());
    }
}
