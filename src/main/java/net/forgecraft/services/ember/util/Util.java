package net.forgecraft.services.ember.util;

import java.net.http.HttpClient;
import java.util.List;

public class Util {

    // TODO move to config
    /**
     * We're basically just acting as a proxy for these mavens so we can download the artifacts
     */
    @Deprecated
    public static final List<String> TRUSTED_MAVENS = List.of(
            "https://maven.blamejared.com",
            "https://modmaven.k-4u.nl",
            "https://maven.saps.dev/releases",
            "https://maven.saps.dev/snapshots",
            "https://maven.nanite.dev/releases",
            "https://maven.nanite.dev/snapshots",
            "https://maven.fabricmc.net",
            "https://maven.creeperhost.net",
            "https://maven.minecraftforge.net",
            "https://maven.neoforged.net/releases",
            "https://repo.spongepowered.org/maven"
    );

    public static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static String stripTrailingSlash(String input) {
        if (input.endsWith("/")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }
}
