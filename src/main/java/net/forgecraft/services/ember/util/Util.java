package net.forgecraft.services.ember.util;

import net.forgecraft.services.ember.Main;
import net.forgecraft.services.ember.app.Services;

import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Util {

    // TODO move to config
    /**
     * A list of known maven repositories to try and resolve maven artifacts against
     */
    @Deprecated
    public static final List<String> KNOWN_MAVENS = List.of(
            "https://api.modrinth.com/maven",
            "https://cursemaven.com",
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
            "https://repo.spongepowered.org/maven",
            "https://maven.uuid.gg/releases"
    );

    //TODO might be wise to use a different http library that allows setting default headers such as User-Agent
    /**
     * @implNote Need to use the builder because the default HTTP client does not follow any redirects.
     * We change this so it does, except for redirects to less secure URLs, i.e. https to http
     *
     * @return a new {@link HttpClient} that follows redirects
     */
    public static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Util.BACKGROUND_EXECUTOR)
                .build();
    }

    public static String stripTrailingSlash(String input) {
        if (input.endsWith("/")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }

    public static Services services() {
        return Main.INSTANCE.services();
    }

    public static final ExecutorService BACKGROUND_EXECUTOR = Executors.newCachedThreadPool(r -> new Thread(r, "Ember Background Worker"));
}
