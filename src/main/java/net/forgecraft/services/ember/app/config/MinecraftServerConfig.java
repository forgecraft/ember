package net.forgecraft.services.ember.app.config;

import java.io.IOException;
import java.nio.file.LinkOption;
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
        var real = root.resolve(name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._]", "_")).toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (!real.startsWith(root)) {
            throw new IOException("Path " + real + " is outside of parent directory " + root);
        }
        return real;
    }
}
