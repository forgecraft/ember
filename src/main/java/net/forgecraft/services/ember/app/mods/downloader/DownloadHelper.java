package net.forgecraft.services.ember.app.mods.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadHelper.class);

    public static boolean saveTo(Path path, byte[] data) throws IOException {
        if (Files.isRegularFile(path)) {
            return false;
        }

        Files.createDirectories(path.getParent());
        Files.write(path, data);
        LOGGER.debug("Successfully saved file to {}", path);
        return true;
    }
}
