package net.forgecraft.services.ember.app.mods.downloader.plain;

import com.google.common.base.Preconditions;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SimpleDownloadInfo implements DownloadInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDownloadInfo.class);
    private final URI url;
    private Hash hash = null;
    private final String fileName;
    private final CompletableFuture<byte[]> download;

    public SimpleDownloadInfo(URI url, HttpClient client) {
        this(url, extractFileNameFromUrl(url), client);
    }

    public SimpleDownloadInfo(URI url, String fileName, HttpClient client) {
        this.url = url;
        this.fileName = fileName;
        this.download = startDownload(client);
    }

    protected CompletableFuture<byte[]> startDownload(HttpClient client) {
        this.printStartMessage();
        return client.sendAsync(HttpRequest.newBuilder(url).build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApplyAsync(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Failed to download " + url + ": Received status code " + response.statusCode());
                    }
                    var bytes = response.body();
                    var calculatedHash = Hash.fromBytes(Hash.Type.SHA256, bytes);

                    // verify hash matches if it exists
                    if (hash != null) {
                        if (!calculatedHash.equals(hash)) {
                            throw new IllegalStateException("Hash mismatch for " + url + ": expected " + hash + ", got " + calculatedHash);
                        }
                    } else {
                        this.hash = calculatedHash;
                    }
                    return bytes;
                });
    }

    protected void printStartMessage() {
        LOGGER.debug("Downloading {}", url);
    }

    @Override
    public URI getUrl() {
        return url;
    }

    @Override
    public @Nullable Hash getSha256() {
        return hash;
    }

    public void setSha256Hash(@Nullable Hash hash) {
        Preconditions.checkArgument(hash == null || hash.type() == Hash.Type.SHA256, "Hash type must be SHA256");
        this.hash = hash;
    }

    @Override
    public boolean isComplete() {
        return download.isDone();
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public CompletableFuture<byte[]> getFileContents() {
        return download;
    }

    @Override
    public void cancel() {
        download.cancel(true);
    }

    @Nullable
    protected static String extractFileNameFromUrl(URI url) {
        var path = url.getPath();
        var lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return path;
        } else if (lastSlash == path.length() - 1) {
            // not a file
            return null;
        }

        return path.substring(lastSlash + 1);
    }
}
