package net.forgecraft.services.ember.app.mods.downloader;

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
            new MavenDownloader(),
            new UrlDownloader()
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
