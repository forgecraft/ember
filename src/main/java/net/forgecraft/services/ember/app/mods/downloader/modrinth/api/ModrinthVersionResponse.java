package net.forgecraft.services.ember.app.mods.downloader.modrinth.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// https://docs.modrinth.com/#tag/project_result_model
public record ModrinthVersionResponse(
        String name,
        String version_number,
        Optional<String> changelog,
        List<VersionDependency> dependencies,
        List<String> game_versions,
        VersionType version_type,
        List<String> loaders,
        boolean featured,
        Optional<VersionStatus> status,
        Optional<String> requested_status,
        String id,
        String project_id,
        String author_id,
        String date_published,
        long downloads,
        List<VersionFile> files
) {
    public enum VersionType {
        RELEASE,
        BETA,
        ALPHA
    }

    public enum VersionStatus {
        LISTED,
        ARCHIVED,
        DRAFT,
        UNLISTED,
        SCHEDULED,
        UNKNOWN
    }

    public record VersionDependency(
            Optional<String> version_id,
            Optional<String> project_id,
            Optional<String> file_name,
            Type dependency_type
    ) {

        public enum Type {
            REQUIRED,
            OPTIONAL,
            INCOMPATIBLE,
            EMBEDDED
        }
    }

    public record VersionFile(
            Map<String, String> hashes,
            URI url,
            String filename,
            boolean primary,
            long size,
            Optional<VersionFileType> file_type
    ) {

        public enum VersionFileType {
            @JsonProperty("required-resource-pack")
            REQUIRED_RESOURCE_PACK,
            @JsonProperty("optional-resource-pack")
            OPTIONAL_RESOURCE_PACK
        }
    }
}
