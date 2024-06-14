package net.forgecraft.services.ember.app.mods.downloader.curseforge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CfWidgetApiResponse(long id, String title) {
}
