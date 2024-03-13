package net.forgecraft.services.ember.app.mods.downloader.modrinth;

import net.forgecraft.services.ember.app.config.ModrinthConfig;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.downloader.plain.SimpleDownloadInfo;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class ModrinthDownloadInfo extends SimpleDownloadInfo {

    private final ModrinthConfig cfg;

    public ModrinthDownloadInfo(URI url, String fileName, @Nullable Hash expectedHash, HttpClient client, ModrinthConfig cfg) {
        super(url, fileName, expectedHash, client);
        this.cfg = cfg;
    }

    @Override
    protected HttpUriRequestBase createRequest() {
        return ModrinthDownloader.addAuthHeader(super.createRequest(), cfg);
    }
}
