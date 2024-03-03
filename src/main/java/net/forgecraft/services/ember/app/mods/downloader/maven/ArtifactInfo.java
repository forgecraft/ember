package net.forgecraft.services.ember.app.mods.downloader.maven;

import net.forgecraft.services.ember.app.mods.downloader.Hash;
import org.jetbrains.annotations.Nullable;

public record ArtifactInfo(String group, String artifact, String version, @Nullable String classifier,
                           String extension) {

    public static ArtifactInfo fromString(String input) {
        var matcher = MavenDownloader.DEPENDENCY_NOTATION_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid dependency notation: " + input);
        }

        // guaranteed to exist
        var group = matcher.group("group");
        var artifact = matcher.group("artifact");
        var version = matcher.group("version");

        // optional
        var classifier = matcher.group("classifier");
        if (classifier != null && classifier.isBlank()) {
            classifier = null;
        }
        var extension = matcher.group("extension");
        if (extension == null) {
            extension = "jar";
        }

        return new ArtifactInfo(group, artifact, version, classifier, extension);
    }

    public String toUrlPath() {
        // group/artifact/version/artifact-version-classifier.extension
        return '/' + group().replace('.', '/') + '/' + artifact() + '/' + version() + '/' + getFileName();
    }

    @SuppressWarnings("deprecation")
    public String toHashFilePath(Hash.Type type) {
        return toUrlPath() + "." + switch (type) {
            case SHA512 -> "sha512";
            case SHA256 -> "sha256";
            case SHA1 -> "sha1";
            case MD5 -> "md5";
        };
    }

    public String getFileName() {
        return artifact() + "-" + version() + (classifier() != null ? "-" + classifier() : "") + "." + extension();
    }

    @Override
    public String toString() {
        return "maven:" + group() + ":" + artifact() + ":" + version() + (classifier() != null ? ":" + classifier() : "") + "@" + extension();
    }
}
