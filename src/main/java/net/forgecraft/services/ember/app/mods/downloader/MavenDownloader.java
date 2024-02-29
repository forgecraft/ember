package net.forgecraft.services.ember.app.mods.downloader;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves a maven artifact classifier (com.example:artifact:version:classifier) to a path, then queries each
 * trusted maven repository for the artifact, downloading it if found.
 */
public class MavenDownloader implements Downloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDownloader.class);

    private final HttpClient client = HttpClient.newHttpClient();

    // TODO: Add more trusted maven repositories
    /**
     * We're basically just acting as a proxy for these mavens so we can download the artifacts
     */
    private static final List<String> TRUSTED_MAVENS = List.of(
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

    @Override
    public @Nullable Path download(String inputData) {
        var resolvedPath = resolvePath(inputData);
        if (resolvedPath == null) {
            return null;
        }

        var artifactPath = lookupArtifact(resolvedPath);
        if (artifactPath == null) {
            return null;
        }

        // It exists! Download it
        var request = HttpRequest.newBuilder()
                .uri(URI.create(artifactPath))
                .GET()
                .build();

        try {
            String fileName = artifactPath.substring(artifactPath.lastIndexOf('/') + 1);
            Path file = Path.of("downloads", fileName);
            LOGGER.debug("Downloading " + artifactPath + " to " + file);
            var response = client.send(request, HttpResponse.BodyHandlers.ofFile(file));
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download artifact", e);
        }

        return null;
    }

    // TODO: Tests!
    @Nullable
    private String resolvePath(String classifier) {
        var parts = classifier.replace("maven:", "").split(":");
        var domain = parts[0].replace(".", "/");

        if (parts.length == 3) {
            return domain + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
        }

        if (parts.length == 4) {
            return domain + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + "-" + parts[3] + ".jar";
        }

        return null;
    }

    /**
     * Iterates through the trusted maven repositories and creates a head request to check if the artifact exists.
     * @param path the resolved path to the artifact
     * @return the path to the artifact if found, null otherwise
     */
    @Nullable
    private String lookupArtifact(String path) {
        for (String maven : TRUSTED_MAVENS) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(maven + "/" + path))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            LOGGER.debug("Looking up " + request.uri());

            try {
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return maven + "/" + path;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to lookup artifact", e);
            }
        }

        return null;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return inputData.startsWith("maven:");
    }
}
