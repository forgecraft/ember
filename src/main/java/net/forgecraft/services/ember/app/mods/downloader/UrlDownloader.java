package net.forgecraft.services.ember.app.mods.downloader;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class UrlDownloader implements Downloader {
    @Override
    public boolean isAcceptable(String inputData) {
        return inputData.startsWith("https://");
    }

    @Override
    public @Nullable Path download(String inputData) {
        System.out.println("UrlDownloader: " + inputData);
        return null;
    }
}
