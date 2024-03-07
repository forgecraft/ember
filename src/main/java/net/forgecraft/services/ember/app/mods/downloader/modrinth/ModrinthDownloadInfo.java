package net.forgecraft.services.ember.app.mods.downloader.modrinth;

import net.forgecraft.services.ember.app.config.ModrinthConfig;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.downloader.plain.SimpleDownloadInfo;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class ModrinthDownloadInfo extends SimpleDownloadInfo {

    private final ModrinthConfig cfg;

    public ModrinthDownloadInfo(URI url, String fileName, @Nullable Hash expectedHash, HttpClient client, ModrinthConfig cfg) {
        super(url, fileName, expectedHash, client);
        this.cfg = cfg;
    }

    @Override
    protected HttpRequest.Builder createRequestBuilder() {
        return ModrinthDownloader.addAuthHeader(super.createRequestBuilder(), cfg);
    }
}
