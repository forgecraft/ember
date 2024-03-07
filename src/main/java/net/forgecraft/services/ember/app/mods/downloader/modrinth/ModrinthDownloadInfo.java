package net.forgecraft.services.ember.app.mods.downloader.modrinth;

import net.forgecraft.services.ember.app.Config;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.downloader.plain.SimpleDownloadInfo;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;

public class ModrinthDownloadInfo extends SimpleDownloadInfo {

    private final Config.ModrinthConfig cfg;

    public ModrinthDownloadInfo(URI url, String fileName, @Nullable Hash expectedHash, Config.ModrinthConfig cfg) {
        super(url, fileName, expectedHash);
        this.cfg = cfg;
    }

    @Override
    protected HttpRequest.Builder createRequestBuilder() {
        return ModrinthDownloader.addAuthHeader(super.createRequestBuilder(), cfg);
    }
}
