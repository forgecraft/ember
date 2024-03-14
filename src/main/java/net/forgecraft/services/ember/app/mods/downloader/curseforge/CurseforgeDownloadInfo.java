package net.forgecraft.services.ember.app.mods.downloader.curseforge;

import io.github.matyrobbrt.curseforgeapi.schemas.mod.Mod;
import net.forgecraft.services.ember.app.config.CurseforgeConfig;
import net.forgecraft.services.ember.app.mods.downloader.plain.SimpleDownloadInfo;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.net.URI;

public class CurseforgeDownloadInfo extends SimpleDownloadInfo {

    private final CurseforgeConfig cfg;
    private final Mod project;
    private final long fileId;

    public CurseforgeDownloadInfo(URI url, HttpClient client, CurseforgeConfig cfg, Mod project, long fileId) {
        super(url, client);
        this.cfg = cfg;
        this.project = project;
        this.fileId = fileId;
    }

    @Override
    protected HttpUriRequestBase createRequest() {
        var request = super.createRequest();
        cfg.accessToken().ifPresent(token -> request.addHeader(CurseForgeDownloader.CURSEFORGE_API_KEY_HEADER, token));
        return request;
    }

    public Mod getProject() {
        return project;
    }

    public long getFileId() {
        return fileId;
    }
}
