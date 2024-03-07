package net.forgecraft.services.ember.app.mods.downloader.modrinth;

import net.forgecraft.services.ember.app.Config;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.downloader.modrinth.api.ModrinthVersionResponse;
import net.forgecraft.services.ember.util.serialization.JsonBodyHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A downloader for Modrinth
 * <p>
 * Expected inputs:
 * - https://(www.)modrinth.com/mod/emi/version/1.1.2+1.20.4+neoforge // We need to look this up with the api
 * - 8qHA9xh2 // This is truly unique and we can use the api to look it up
 * - https://cdn.modrinth.com/data/fRiHVvU7/versions/8qHA9xh2/emi-1.1.2%2B1.20.4%2Bneoforge.jar // We can use this directly. This should just fallback to the url downloader. There is no need to have a separate downloader for this.
 */
public class ModrinthDownloader implements Downloader {

    private static final String API_URL = "https://api.modrinth.com/v2";
    private static final Pattern MODRINTH_ID_PATTERN = Pattern.compile("modrinth:((?<project>\\w+):)?(?<version>\\w+)");
    private static final Pattern MODRINTH_URL_PATTERN = Pattern.compile("https://(www\\.)?modrinth\\.com/mod/(?<project>.+)/version/(?<version>.+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(ModrinthDownloader.class);

    private final Config.ModrinthConfig cfg;
    private final Supplier<HttpClient> clientFactory;

    public ModrinthDownloader(Config.ModrinthConfig cfg, Supplier<HttpClient> clientFactory) {
        this.cfg = cfg;
        this.clientFactory = clientFactory;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return MODRINTH_ID_PATTERN.asMatchPredicate()
                .or(MODRINTH_URL_PATTERN.asMatchPredicate())
                .test(inputData);
    }

    @Override
    public @Nullable DownloadInfo startDownload(String inputData) {
        var client = clientFactory.get();

        String project = null;
        String version = null;

        var matcher = MODRINTH_ID_PATTERN.matcher(inputData);
        if (matcher.matches()) {
            project = matcher.group("project");
            version = matcher.group("version");


        } else {
            matcher = MODRINTH_URL_PATTERN.matcher(inputData);
            if (matcher.matches()) {
                project = matcher.group("project");
                version = matcher.group("version");
            }
        }

        if (version == null) {
            LOGGER.error("Failed to parse modrinth version from {}", inputData);
            return null;
        }

        HttpRequest.Builder builder = addAuthHeader(HttpRequest.newBuilder(), cfg);
        if (project != null) {
            // https://docs.modrinth.com/#tag/versions/operation/getVersionFromIdOrNumber
            builder.uri(URI.create("%s/project/%s/version/%s".formatted(API_URL, project, version)));
        } else {
            // https://docs.modrinth.com/#tag/versions/operation/getVersion
            builder.uri(URI.create("%s/version/%s".formatted(API_URL, version)));
        }
        var request = builder.build();

        try {
            var response = client.send(request, JsonBodyHandler.of(ModrinthVersionResponse.class));
            if (response.statusCode() != 200) {
                LOGGER.error("Failed to send request to modrinth: Received HTTP Status {} for {}", response.statusCode(), request.uri());
                return null;
            }

            var modrinthVersion = response.body();
            if (modrinthVersion == null) {
                LOGGER.error("Received empty response for {}", request.uri());
                return null;
            }

            var primaryFile = modrinthVersion.files().stream().filter(ModrinthVersionResponse.VersionFile::primary).findFirst().orElse(null);
            if (primaryFile == null) {
                LOGGER.error("No primary file found for modrinth version {}", modrinthVersion.id());
                return null;
            }

            var hash = Optional.ofNullable(primaryFile.hashes().get("sha512"))
                    .map(raw -> Hash.fromString(Hash.Type.SHA512, raw))
                    .orElse(null);

            // TODO parse and download required dependencies
            // https://docs.modrinth.com/#tag/project_result_model

            var dl = new ModrinthDownloadInfo(primaryFile.url(), primaryFile.filename(), hash, cfg);
            dl.start(client);
            return dl;

        } catch (UncheckedIOException | IOException | InterruptedException e) {
            LOGGER.error("Failed to look up modrinth version {} at {}", version, request.uri(), e);
        }

        return null;
    }

    static HttpRequest.Builder addAuthHeader(HttpRequest.Builder builder, Config.ModrinthConfig cfg) {
        cfg.accessToken().ifPresent(token -> builder.header("Authorization", token));
        return builder;
    }
}
