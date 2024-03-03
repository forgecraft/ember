package net.forgecraft.services.ember.app.mods.downloader;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface DownloadInfo {

    URI getUrl();

    /**
     * Get the sha256 hash of the downloaded file.<br>
     * If this method is called before {@link DownloadInfo#isComplete()} returns true, and the hash is not known beforehand, it will return {@code null}.
     */
    @Nullable
    Hash getSha256();

    boolean isComplete();

    String getFileName();

    CompletableFuture<byte[]> getFileContents();

    void cancel();
}
