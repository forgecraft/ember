package net.forgecraft.services.ember.app.mods.downloader;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface Downloader {
    /**
     * Check if the input data is acceptable for this downloader to handle
     *
     * @param inputData The input data
     * @return True if the input data is acceptable
     */
    boolean isAcceptable(String inputData);

    /**
     * Attempt to download from the given input data
     *
     * @param inputData The input data
     * @return The path to the downloaded file
     */
    @Nullable
    Path download(String inputData);
}
