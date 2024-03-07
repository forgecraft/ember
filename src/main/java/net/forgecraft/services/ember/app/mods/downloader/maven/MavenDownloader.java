package net.forgecraft.services.ember.app.mods.downloader.maven;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves a maven artifact classifier (maven:group:artifact:version:classifier@extension) to a path, then queries each
 * known maven repository for the artifact, downloading it if found.
 */
public class MavenDownloader implements Downloader {

    private final Map<String, String> artifactLocatorCache = new Object2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDownloader.class);
    static final Pattern DEPENDENCY_NOTATION_PATTERN = Pattern.compile("maven:(?<group>[\\w.-]+):(?<artifact>[\\w.-]+):(?<version>[\\w.+-]+)(:(?<classifier>[\\w-]+))?(@(?<extension>[\\w-]+))?");

    private final Supplier<HttpClient> clientFactory;
    private final List<String> knownMavenUrls;

    public MavenDownloader(Supplier<HttpClient> clientFactory, List<String> knownMavenUrls) {
        this.clientFactory = clientFactory;
        this.knownMavenUrls = knownMavenUrls.stream().map(Util::stripTrailingSlash).toList();
    }

    //FIXME sending a HEAD request first is pointless, just try downloading immediately

    /**
     * Iterates through the known maven repositories and creates a head request to check if the artifact exists.
     *
     * @return the URL of the first maven that contains the artifact, or {@code null} if not found
     */
    @Nullable
    private String lookupArtifact(ArtifactInfo info, HttpClient client) {
        var cacheKey = info.group() + ":" + info.artifact();

        // check cache first to not need to query all mavens all the time
        String cachedMaven;
        synchronized (artifactLocatorCache) {
            cachedMaven = artifactLocatorCache.get(cacheKey);
        }

        if (cachedMaven != null && checkArtifactExists(cachedMaven, info, client)) {
            return cachedMaven;
        }

        for (String maven : knownMavenUrls) {
            if (checkArtifactExists(maven, info, client)) {

                // update cache
                synchronized (artifactLocatorCache) {
                    artifactLocatorCache.put(cacheKey, maven);
                }

                return maven;
            }
        }

        return null;
    }

    private static boolean checkArtifactExists(String maven, ArtifactInfo artifact, HttpClient client) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(maven + "/" + artifact.toUrlPath()))
                .HEAD()
                .build();
        LOGGER.trace("Checking {}", request.uri());

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                LOGGER.debug("Found artifact {} at {}", artifact, maven);
                return true;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to look up artifact {} in maven {}", artifact, maven, e);
        }

        return false;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return DEPENDENCY_NOTATION_PATTERN.asMatchPredicate().test(inputData);
    }

    @Override
    public @Nullable DownloadInfo startDownload(String inputData) {
        var client = clientFactory.get();
        var artifact = ArtifactInfo.fromString(inputData);
        var mavenUrl = lookupArtifact(artifact, client);
        if (mavenUrl == null) {
            LOGGER.error("Unable to locate artifact: {}", inputData);
            return null;
        }

        // It exists! try to download the hash first
        @Nullable Hash hash = downloadHashFromUrl(mavenUrl, artifact, Hash.Type.SHA512, client);
        var dl = new MavenDownloadInfo(mavenUrl, artifact, hash);
        dl.start(client);
        return dl;
    }

    @Nullable
    private static Hash downloadHashFromUrl(String mavenUrl, ArtifactInfo artifact, Hash.Type hashType, HttpClient client) {
        try {
            var url = URI.create(mavenUrl + "/" + artifact.toHashFilePath(hashType));
            LOGGER.trace("Trying to download hash file {}", url);
            var request = HttpRequest.newBuilder()
                    .uri(url)
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var rawHash = response.body().lines().collect(Collectors.joining()).strip();
                LOGGER.trace("Hash for {} is {}", artifact, rawHash);
                return Hash.fromString(hashType, rawHash);
            }
        } catch (Exception e) {
            // ignore exception, we'll just download the file without the hash
            LOGGER.debug("Failed to download {} hash for {}", hashType, artifact, e);
        }
        return null;
    }
}
