package net.forgecraft.services.ember.app.config;

import java.nio.file.Path;

public record GeneralConfig(
        Path storageDir,
        Path databasePath
) {

    public static GeneralConfig create() {
        return new GeneralConfig(Path.of("data/files"), Path.of("data/sqlite.db"));
    }
}
