package net.forgecraft.services.ember.app.mods.downloader;

import net.forgecraft.services.ember.util.Util;
import net.forgecraft.services.ember.app.mods.downloader.curseforge.CurseForgeDownloader;
import net.forgecraft.services.ember.app.mods.downloader.maven.MavenDownloader;
import net.forgecraft.services.ember.app.mods.downloader.modrinth.ModrinthDownloader;
import net.forgecraft.services.ember.app.mods.downloader.plain.PlainUrlDownloader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum DownloaderFactory {
    INSTANCE;

    /**
     * The order of the downloaders is important as it will be used to determine which downloader to use first.
     * If there is no special matches, we'll fall back to the url downloader.
     */
    private final List<Downloader> downloaders = List.of(
            new ModrinthDownloader(),
            new CurseForgeDownloader(),
            new MavenDownloader(Util::newHttpClient, Util.TRUSTED_MAVENS, false),
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
}
