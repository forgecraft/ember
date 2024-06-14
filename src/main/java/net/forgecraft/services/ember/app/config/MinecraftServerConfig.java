package net.forgecraft.services.ember.app.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public record MinecraftServerConfig(
        String name,
        Path path,
        long uploadChannel
) {

    public static MinecraftServerConfig create() {
        return new MinecraftServerConfig("Server 1", Path.of("server_1"), -1);
    }

    public Path getNameAsPath(Path root) throws IOException {
        // sadly cannot use Path#toRealPath because it doesn't work with non-existent paths
        var normalized = root.resolve(name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._]", "_")).normalize().toAbsolutePath();
        if (!normalized.startsWith(root.normalize().toAbsolutePath())) {
            throw new IOException("Path " + normalized + " is outside of parent directory " + root);
        }
        return normalized;
    }
}
