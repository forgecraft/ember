package net.forgecraft.services.ember.app.mods.downloader.maven;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.util.Util;
import net.forgecraft.services.ember.util.serialization.StatusCodeOnlyBodyHandlerApache;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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

        var request = new HttpHead(URI.create(maven + "/" + artifact.toUrlPath()));

        LOGGER.trace("Checking {}", request.getRequestUri());

        try {
            var responseStatus = client.execute(request, StatusCodeOnlyBodyHandlerApache.INSTANCE);
            if (responseStatus == HttpStatus.SC_OK) {
                LOGGER.debug("Found artifact {} at {}", artifact, maven);
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to look up artifact {} in maven {}", artifact, maven, e);
        }

        return false;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return DEPENDENCY_NOTATION_PATTERN.asMatchPredicate().test(inputData);
    }

    @Override
    public @Nullable DownloadInfo createDownloadInstance(String inputData) {
        var client = clientFactory.get();
        var artifact = ArtifactInfo.fromString(inputData);
        var mavenUrl = lookupArtifact(artifact, client);
        if (mavenUrl == null) {
            LOGGER.error("Unable to locate artifact: {}", inputData);
            return null;
        }

        // It exists! try to download the hash first
        @Nullable Hash hash = downloadHashFromUrl(mavenUrl, artifact, Hash.Type.SHA512, client);
        return new MavenDownloadInfo(mavenUrl, artifact, hash, client);
    }

    @Nullable
    private static Hash downloadHashFromUrl(String mavenUrl, ArtifactInfo artifact, Hash.Type hashType, HttpClient client) {
        try {
            var url = URI.create(mavenUrl + "/" + artifact.toHashFilePath(hashType));
            LOGGER.trace("Trying to download hash file {}", url);
            var request = new HttpGet(url);
            var rawHash = client.execute(request, new BasicHttpClientResponseHandler()).strip();
            LOGGER.trace("Hash for {} is {}", artifact, rawHash);
            return Hash.fromString(hashType, rawHash);
        } catch (IOException e) {
            // ignore exception, we'll just download the file without the hash
            LOGGER.debug("Failed to download {} hash for {}", hashType, artifact, e);
        }
        return null;
    }
}
