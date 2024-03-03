package net.forgecraft.services.ember.app.mods.downloader.maven;

import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.downloader.plain.SimpleDownloadInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;

class MavenDownloadInfo extends SimpleDownloadInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDownloadInfo.class);
    private final ArtifactInfo artifact;

    public MavenDownloadInfo(String mavenURL, ArtifactInfo artifact, @Nullable Hash sha256, HttpClient client) {
        super(URI.create(mavenURL + "/" + artifact.toUrlPath()), artifact.getFileName(), client);
        this.setSha256Hash(sha256);
        this.artifact = artifact;
    }

    @Override
    protected void printStartMessage() {
        LOGGER.debug("Downloading {} from {}", artifact.getFileName(), getUrl());
    }

    public ArtifactInfo getArtifact() {
        return artifact;
    }
}
