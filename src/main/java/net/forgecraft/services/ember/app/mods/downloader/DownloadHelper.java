package net.forgecraft.services.ember.app.mods.downloader;

import com.google.common.base.Preconditions;
import net.forgecraft.services.ember.app.config.GeneralConfig;
import net.forgecraft.services.ember.app.config.MinecraftServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DownloadHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadHelper.class);

    public static void saveTo(Path path, byte[] data) throws IOException {
        Files.createDirectories(path.getParent());

        // throw if already exists
        Files.write(path, data, StandardOpenOption.CREATE_NEW);

        LOGGER.debug("Successfully saved file to {}", path);
    }

    public static Path getCacheFileName(GeneralConfig cfg, MinecraftServerConfig serverCfg, Hash hash, String fileName) throws IOException {
        Preconditions.checkArgument(hash.type() == Hash.Type.SHA512, "Hash type must be SHA-512");
        return serverCfg.getNameAsPath(cfg.storageDir()).resolve(hash.stringValue()).resolve(fileName);
    }
}
