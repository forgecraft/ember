package net.forgecraft.services.ember.app.mods.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadHelper {

    public static boolean saveTo(Path path, byte[] data) throws IOException {
        if (Files.isRegularFile(path)) {
            return false;
        }

        Files.createDirectories(path.getParent());
        Files.write(path, data);
        return true;
    }
}
