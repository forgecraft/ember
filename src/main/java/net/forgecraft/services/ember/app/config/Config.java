package net.forgecraft.services.ember.app.config;

import net.forgecraft.services.ember.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Config {

    private final GeneralConfig general = GeneralConfig.create();
    private final DiscordConfig discord = DiscordConfig.create();
    private final CurseforgeConfig curseforge = CurseforgeConfig.create();
    private final ModrinthConfig modrinth = ModrinthConfig.create();
    private final List<MinecraftServerConfig> minecraftServers = List.of(MinecraftServerConfig.create());


    public static Config load(Path path) {
        try (var stream = loadFromPath(path)) {
            return Util.JACKSON_MAPPER.readValue(stream, Config.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config file", e);
        }
    }

    private static InputStream loadFromPath(Path path) throws IOException {
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
            try (var writer = Files.newBufferedWriter(path)) {
                Util.JACKSON_MAPPER.writeValue(writer, new Config());
            }
        }

        // We exist so just read it
        return Files.newInputStream(path);
    }

    public GeneralConfig getGeneral() {
        return general;
    }

    public DiscordConfig getDiscord() {
        return discord;
    }

    public CurseforgeConfig getCurseforge() {
        return curseforge;
    }

    public ModrinthConfig getModrinth() {
        return modrinth;
    }

    public List<MinecraftServerConfig> getMinecraftServers() {
        return minecraftServers;
    }

}
