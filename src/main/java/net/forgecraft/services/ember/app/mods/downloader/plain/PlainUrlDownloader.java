package net.forgecraft.services.ember.app.mods.downloader.plain;

import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.function.Supplier;

public class PlainUrlDownloader implements Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainUrlDownloader.class);
    private final Supplier<HttpClient> clientFactory;
    private final boolean allowInsecure;

    public PlainUrlDownloader(Supplier<HttpClient> clientFactory, boolean allowInsecure) {
        this.clientFactory = clientFactory;
        this.allowInsecure = allowInsecure;
        if (allowInsecure) {
            LOGGER.warn("Downloading from insecure URLs is allowed.");
        }
    }

    @Override
    public boolean isAcceptable(String inputData) {
        //noinspection HttpUrlsUsage
        return inputData.startsWith("https://") || (allowInsecure && inputData.startsWith("http://"));
    }

    @Override
    public @Nullable DownloadInfo startDownload(String inputData) {
        try {
            var uri = new URI(inputData);
            var dl = new SimpleDownloadInfo(uri);
            dl.start(clientFactory.get());
            return dl;
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to download " + inputData, e);
        }
        return null;
    }
}
