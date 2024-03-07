package net.forgecraft.services.ember.app.mods.downloader;

import net.forgecraft.services.ember.app.mods.downloader.curseforge.CurseForgeDownloader;
import net.forgecraft.services.ember.app.mods.downloader.maven.MavenDownloader;
import net.forgecraft.services.ember.app.mods.downloader.modrinth.ModrinthDownloader;
import net.forgecraft.services.ember.app.mods.downloader.plain.PlainUrlDownloader;
import net.forgecraft.services.ember.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public enum DownloaderFactory {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloaderFactory.class);

    /**
     * The order of the downloaders is important as it will be used to determine which downloader to use first.
     * If there is no special matches, we'll fall back to the url downloader.
     */
    private final List<Downloader> downloaders = List.of(
            new ModrinthDownloader(Util.services().getConfig().getModrinth(), Util::newHttpClient),
            new CurseForgeDownloader(),
            new MavenDownloader(Util::newHttpClient, Util.KNOWN_MAVENS),
            new PlainUrlDownloader(Util::newHttpClient, false)
    );

    @Nullable
    public Downloader factory(String inputData) {
        for (var downloader : downloaders) {
            if (downloader.isAcceptable(inputData)) {
                return downloader;
            }
        }

        return null;
    }

    @Nullable
    public DownloadInfo tryDownload(String inputData) {
        var downloader = factory(inputData);

        if (downloader != null) {
            LOGGER.debug("found valid download: {}", inputData);
            return downloader.startDownload(inputData);
        }

        return null;
    }
}
