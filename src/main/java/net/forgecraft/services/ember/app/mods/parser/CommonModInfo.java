package net.forgecraft.services.ember.app.mods.parser;

import java.util.Optional;

public record CommonModInfo(
        Type type,
        String id,
        String name,
        String version,
        Optional<String> projectUrl,
        Optional<String> issuesUrl
) {
    public enum Type {
        NEOFORGE,
        FABRIC,
        QUILT
    }
}
