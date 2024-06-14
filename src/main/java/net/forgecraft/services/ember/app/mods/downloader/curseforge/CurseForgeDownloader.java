package net.forgecraft.services.ember.app.mods.downloader.curseforge;

import io.github.matyrobbrt.curseforgeapi.CurseForgeAPI;
import io.github.matyrobbrt.curseforgeapi.request.Requests;
import io.github.matyrobbrt.curseforgeapi.util.CurseForgeException;
import net.forgecraft.services.ember.app.config.CurseforgeConfig;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import net.forgecraft.services.ember.util.serialization.JsonResponseHandler;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A downloader for CurseForge
 * <p>
 * Expected inputs:
 * - curseforge:simplygraves:5126737
 * - curseforge:620112:5126737
 * - https://www.curseforge.com/minecraft/mc-mods/simply-graves/files/5126737
 * - https://www.curseforge.com/minecraft/mc-mods/simply-graves/download/5126737
 * - https://legacy.curseforge.com/minecraft/mc-mods/simply-graves/download/5126737
 * - https://legacy.curseforge.com/minecraft/mc-mods/simply-graves/download/5126737
 * Inputs we DONT handle:
 * - https://mediafilez.forgecdn.net/files/5126/737/simplygraves-1.19.2-1.1.0-build.19.jar // already handled by the plain URL downloader
 * - https://www.curseforge.com/projects/620112 // project page, not a file; adding a file to that URL makes it invalid
 */
public class CurseForgeDownloader implements Downloader {

    public static final String CURSEFORGE_API_KEY_HEADER = "X-Api-Key";

    private static final Pattern CURSEFORGE_ID_PATTERN = Pattern.compile("(?:curseforge|cf):(?:(?<slug>\\w+):)?(?<fileID>\\d+)");
    private static final Pattern CURSEFORGE_URL_PATTERN = Pattern.compile("https://(?:legacy|www)\\.curseforge\\.com/minecraft/mc-mods/(?<slug>.+)/(?:files|download)/(?<fileID>\\d+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeDownloader.class);

    private final CurseforgeConfig cfg;
    private final Supplier<HttpClient> clientFactory;
    private final CurseForgeAPI apiClient;

    public CurseForgeDownloader(CurseforgeConfig cfg, Supplier<HttpClient> clientFactory) {
        this.cfg = cfg;
        this.clientFactory = clientFactory;
        CurseForgeAPI apiClient;
        try {
            apiClient = CurseForgeAPI.builder().apiKey(cfg.accessToken().orElseThrow(() -> new LoginException("No access token provided in config"))).build();
        } catch (LoginException e) {
            apiClient = null;
            LOGGER.warn("Failed to login to CurseForge API: {}", e.getMessage());
        }
        this.apiClient = apiClient;
    }

    @Override
    public boolean isAcceptable(String inputData) {
        return CURSEFORGE_ID_PATTERN.asMatchPredicate()
                .or(CURSEFORGE_URL_PATTERN.asMatchPredicate())
                .test(inputData);
    }

    @Override
    public @Nullable DownloadInfo createDownloadInstance(String inputData) {
        if (this.apiClient == null) {
            LOGGER.error("Unable to download {}: Curseforge API support is not available, check startup log for details!", inputData);
            return null;
        }

        // parse input
        String slug = null;
        long fileID = -1;
        try {
            var matcher = CURSEFORGE_ID_PATTERN.matcher(inputData);
            if (matcher.matches()) {
                slug = matcher.group("slug");
                fileID = Long.parseUnsignedLong(matcher.group("fileID"));
            } else {
                matcher = CURSEFORGE_URL_PATTERN.matcher(inputData);
                if (matcher.matches()) {
                    slug = matcher.group("slug");
                    fileID = Long.parseUnsignedLong(matcher.group("fileID"));
                }
            }
        } catch (NumberFormatException ignore) {
        }
        if (slug == null || fileID < 0) {
            LOGGER.error("Failed to parse Curseforge version from {}", inputData);
            return null;
        }

        // look up project ID since we only have the slug and CF's own API does not provide a mapping from slug to project ID
        long projectID;
        try {
            var client = clientFactory.get();
            var cfwidgetResponse = client.execute(new HttpGet("https://api.cfwidget.com/minecraft/mc-mods/%s".formatted(slug)), JsonResponseHandler.of(CfWidgetApiResponse.class));
            LOGGER.debug("Found project {}: {}", cfwidgetResponse.id(), cfwidgetResponse.title());
            projectID = cfwidgetResponse.id();
        } catch (IOException e) {
            LOGGER.error("Failed to look up project ID from CfWdiget for input {}", inputData, e);
            return null;
        }

        // look up project properties via curseforge's API
        try {
            var response = this.apiClient.makeRequest(Requests.getMod((int) projectID));
            if (response.isEmpty()) {
                LOGGER.error("Unable to look up project ID for {}", projectID);
                return null;
            }
            var mod = response.get();
            if (!mod.allowModDistribution()) {
                LOGGER.error("Mod {} does not allow third party distribution", mod.name());
                //TODO call back to uploader to react with a lock symbol
                return null;
            }

            var downloadUrlResponse = this.apiClient.makeRequest(Requests.getModFileDownloadURL((int) projectID, (int) fileID));
            if (downloadUrlResponse.isEmpty()) {
                LOGGER.error("Unable to look up download URL for file {}", fileID);
                return null;
            }
            var downloadUrl = URI.create(downloadUrlResponse.get());

            return new CurseforgeDownloadInfo(downloadUrl, clientFactory.get(), cfg, mod, fileID);
        } catch (CurseForgeException e) {
            LOGGER.error("Failed to look up mod info for {}", inputData, e);
            return null;
        }
    }
}
