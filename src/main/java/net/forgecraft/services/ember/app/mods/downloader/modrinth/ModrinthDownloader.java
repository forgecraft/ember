package net.forgecraft.services.ember.app.mods.downloader.modrinth;

import net.forgecraft.services.ember.app.mods.downloader.Downloader;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * A downloader for Modrinth
 * <p>
 * Expected inputs:
 * - https://modrinth.com/mod/emi/version/1.1.2+1.20.4+neoforge // We need to look this up with the api
 * - 8qHA9xh2 // This is truly unique and we can use the api to look it up
 * - https://cdn.modrinth.com/data/fRiHVvU7/versions/8qHA9xh2/emi-1.1.2%2B1.20.4%2Bneoforge.jar // We can use this directly. This should just fallback to the url downloader. There is no need to have a separate downloader for this.
 */
public class ModrinthDownloader implements Downloader {
    @Override
    public boolean isAcceptable(String inputData) {
        return inputData.startsWith("https://modrinth.com") || inputData.startsWith("https://www.modrinth.com") || inputData.startsWith("modrinth:");
    }

    @Override
    public @Nullable Path download(String inputData) {
        System.out.println("ModrinthDownloader: " + inputData);
        return null;
    }
}
