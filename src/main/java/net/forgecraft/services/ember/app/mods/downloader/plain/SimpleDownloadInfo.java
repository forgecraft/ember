package net.forgecraft.services.ember.app.mods.downloader.plain;

import com.google.common.base.Preconditions;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.util.Util;
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
    private Hash hashSha512;
    private final String fileName;
    @Nullable
    private CompletableFuture<byte[]> download;
    private final HttpClient client;

    public SimpleDownloadInfo(URI url, HttpClient client) {
        this(url, extractFileNameFromUrl(url), null, client);
    }

    public SimpleDownloadInfo(URI url, String fileName, @Nullable Hash expectedHash, HttpClient client) {
        this.url = url;
        this.fileName = fileName;
        this.client = client;
        setSha512(expectedHash);
    }

    protected CompletableFuture<byte[]> startDownload() {
        return CompletableFuture.runAsync(this::printStartMessage, Util.BACKGROUND_EXECUTOR)
                .thenComposeAsync(aVoid -> client.sendAsync(createRequestBuilder().build(), HttpResponse.BodyHandlers.ofByteArray()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Failed to download " + url + ": Received status code " + response.statusCode());
                    }
                    var bytes = response.body();
                    var calculatedHash = Hash.fromBytes(Hash.Type.SHA512, bytes);

                    // verify hash matches if it exists
                    if (hashSha512 != null) {
                        if (!calculatedHash.equals(hashSha512)) {
                            throw new IllegalStateException("Hash mismatch for " + url + ": expected " + hashSha512 + ", got " + calculatedHash);
                        }
                    } else {
                        this.hashSha512 = calculatedHash;
                    }
                    return bytes;
                });
    }

    protected void printStartMessage() {
        LOGGER.debug("Downloading {}", url);
    }

    protected HttpRequest.Builder createRequestBuilder() {
        return HttpRequest.newBuilder(url);
    }

    @Override
    public URI getUrl() {
        return url;
    }

    @Override
    public @Nullable Hash getSha512() {
        return hashSha512;
    }

    public void setSha512(@Nullable Hash hash) {
        Preconditions.checkArgument(hash == null || hash.type() == Hash.Type.SHA512, "Hash type must be SHA-512");
        this.hashSha512 = hash;
    }

    @Override
    public boolean isComplete() {
        return download != null && download.isDone();
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public CompletableFuture<byte[]> getFileContents() {
        if (download == null) {
            start();
        }

        return download;
    }

    @Override
    public void start() {
        this.download = startDownload();
    }

    @Override
    public void cancel() {
        if (download != null) {
            download.cancel(true);
        }
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
