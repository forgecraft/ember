package net.forgecraft.services.ember.app.mods.downloader.plain;

import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import org.apache.hc.client5.http.classic.HttpClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
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
    public @Nullable DownloadInfo createDownloadInstance(String inputData) {
        try {
            var uri = new URI(inputData);
            return new SimpleDownloadInfo(uri, clientFactory.get());
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to download " + inputData, e);
        }
        return null;
    }
}
