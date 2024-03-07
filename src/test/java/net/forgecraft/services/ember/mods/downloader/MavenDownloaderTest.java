package net.forgecraft.services.ember.mods.downloader;

import net.forgecraft.services.ember.app.mods.downloader.DownloaderFactory;
import net.forgecraft.services.ember.app.mods.downloader.maven.MavenDownloader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: Mock file system or something like that to test the actual download process.
 */
public class MavenDownloaderTest {
    static final List<String> KNOWN_GOOD_TARGETS = List.of(
            "net.neoforged:neoforge:20.4.173:installer",
            "net.fabricmc.fabric-api:fabric-api:0.91.1+1.20.4",
            "mezz.jei:jei-1.20.2-common-api:16.0.0.28"
    );

    @Test
    public void resolvesToKnownTrustedMaven() {
        for (var target : KNOWN_GOOD_TARGETS) {
            var joinedInputData = "maven:%s".formatted(target);

            var downloader = DownloaderFactory.INSTANCE.factory(joinedInputData);

            assertInstanceOf(MavenDownloader.class, downloader);

            var download = downloader.createDownloadInstance(joinedInputData);
            assertNotNull(download);
        }
    }
}
