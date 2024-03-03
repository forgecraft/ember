package net.forgecraft.services.ember.app.mods.downloader.maven;

import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.util.Util;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves a maven artifact classifier (com.example:artifact:version:classifier) to a path, then queries each
 * trusted maven repository for the artifact, downloading it if found.
 */
public class MavenDownloader implements Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDownloader.class);
    static final Pattern DEPENDENCY_NOTATION_PATTERN = Pattern.compile("(maven:)?(?<group>[\\w.-]+):(?<artifact>[\\w.-]+):(?<version>[\\w.+-]+)(:(?<classifier>[\\w-]+))?(@(?<extension>[\\w-]+))?");

    private final Supplier<HttpClient> clientFactory;
    private final List<String> mavenUrls;

    public MavenDownloader(Supplier<HttpClient> clientFactory, List<String> mavenUrls, boolean allowInsecure) {
        this.clientFactory = clientFactory;
        var stream = mavenUrls.stream().map(Util::stripTrailingSlash);
        if (allowInsecure) {
            LOGGER.warn("Downloading from insecure maven repositories is allowed.");
        } else {
            stream = stream.filter(url -> {
                if (!url.startsWith("https://")) {
                    LOGGER.warn("Ignoring insecure maven repository: " + url);
                    return false;
                }
                return true;
            });
        }
        this.mavenUrls = stream.toList();
    }

    /**
     * Iterates through the trusted maven repositories and creates a head request to check if the artifact exists.
     *
     * @return the URL of the first maven that contains the artifact, or {@code null} if not found
     */
    @Nullable
    private String lookupArtifact(ArtifactInfo info, HttpClient client) {
        for (String maven : mavenUrls) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(maven + "/" + info.toUrlPath()))
                    .HEAD()
                    .build();

            LOGGER.debug("Looking up " + request.uri());

            try {
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return maven;
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Failed to look up artifact in maven " + maven, e);
            }
        }

        return null;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return mavenUrls.stream().anyMatch(inputData::startsWith) || DEPENDENCY_NOTATION_PATTERN.asMatchPredicate().test(inputData);
    }

    @Override
    public @Nullable DownloadInfo startDownload(String inputData) {
        var client = clientFactory.get();
        ArtifactInfo artifact;
        String mavenUrl = null;

        //TODO move to separate method and add test cases
        try {
            // we can safely accept http URLs here as if they are disabled they will not be matched by this downloader anyway
            //noinspection HttpUrlsUsage
            if (inputData.startsWith("https://") || inputData.startsWith("http://")) {
                // at this point, this is guaranteed to exist
                for (String maven : mavenUrls) {
                    if (inputData.startsWith(maven)) {
                        mavenUrl = maven;
                        break;
                    }
                }
                if (mavenUrl == null) {
                    throw new IllegalStateException("URL does not match any trusted maven: " + inputData + ", this should never happen!");
                }

                var path = inputData.substring(mavenUrl.length() + 1);
                artifact = parseArtifactFromUrlPath(path);
            } else {
                artifact = ArtifactInfo.fromString(inputData);
                mavenUrl = lookupArtifact(artifact, client);
                if (mavenUrl == null) {
                    throw new IllegalStateException("Artifact not found in any maven: " + artifact);
                }
            }
        } catch (IllegalStateException e) {
            LOGGER.error("Unable to locate artifact: " + e.getMessage());
            return null;
        }

        // It exists! try to download the hash first
        Hash hash = downloadHashFromUrl(mavenUrl, artifact, Hash.Type.SHA256, client);
        return new MavenDownloadInfo(mavenUrl, artifact, hash, client);
    }

    @Nullable
    private static Hash downloadHashFromUrl(String mavenUrl, ArtifactInfo artifact, Hash.Type hashType, HttpClient client) {
        try {
            var url = URI.create(mavenUrl + "/" + artifact.toHashFilePath(hashType));
            var request = HttpRequest.newBuilder()
                    .uri(url)
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Hash.fromString(Hash.Type.SHA256, response.body().lines().collect(Collectors.joining()).strip());
            }
        } catch (Exception e) {
            // ignore exception, we'll just download the file without the hash
            LOGGER.debug("Failed to download {} hash for {}", hashType, artifact, e);
        }
        return null;
    }

    //TODO tests
    @VisibleForTesting
    public static ArtifactInfo parseArtifactFromUrlPath(String path) {
        var split = path.split("/");
        if (split.length < 4) {
            throw new IllegalArgumentException("Maven URL does not contain enough path components for group + artifact + version: " + path);
        }

        var last = split[split.length - 1];
        var version = split[split.length - 2];

        var pattern = Pattern.compile("(?<artifact>[\\w.-]+)-(?<versionAndClassifier>[\\w.+-]+)\\.(?<extension>[\\w-]+)");

        var matcher = pattern.matcher(last);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid maven file name: " + path);
        }

        var artifactName = matcher.group("artifact");
        var versionAndClassifier = matcher.group("versionAndClassifier");
        var extension = matcher.group("extension");

        var classifier = versionAndClassifier.substring(version.length() + 1);
        if (classifier.isBlank()) {
            classifier = null;
        }

        var replaced = path.replace('/', '.');
        var group = replaced.substring(0, replaced.indexOf(artifactName) - 1);
        return new ArtifactInfo(group, artifactName, version, classifier, extension);
    }

}
