package net.forgecraft.services.ember.app.mods;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.Pair;
import net.forgecraft.services.ember.app.config.GeneralConfig;
import net.forgecraft.services.ember.app.config.MinecraftServerConfig;
import net.forgecraft.services.ember.app.mods.downloader.DownloadHelper;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.parser.CommonModInfo;
import net.forgecraft.services.ember.app.mods.parser.ModInfoParser;
import net.forgecraft.services.ember.db.schema.tables.records.ModFilesRecord;
import net.forgecraft.services.ember.util.Util;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.forgecraft.services.ember.db.schema.Tables.*;

public class ModFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModFileManager.class);

    public static CompletableFuture<Void> handleDownload(Hash sha512, Path filePath, User discordUser, long messageId) {
        return recordDownload(sha512, filePath, discordUser).thenAccept(modInfo -> {
            try (var db = Util.services().getDbConnection()) {
                db.insertInto(APPROVAL_QUEUE,
                                APPROVAL_QUEUE.MESSAGE_ID,
                                APPROVAL_QUEUE.MOD_FILE_ID
                        )
                        .values(messageId, modInfo.right())
                        .execute();
            }
        });
    }

    public static CompletableFuture<Pair<CommonModInfo, Integer>> recordDownload(Hash sha512, Path filePath, User discordUser) {
        Preconditions.checkArgument(sha512.type() == Hash.Type.SHA512, "Hash type must be SHA-512");
        return CompletableFuture.supplyAsync(() -> {

            var snowflake = discordUser.getId();
            var userDisplayName = discordUser.getName();

            List<CommonModInfo> modInfoList;
            try {
                modInfoList = ModInfoParser.parse(filePath);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read mod file", e);
            }

            if (modInfoList.isEmpty()) {
                throw new IllegalArgumentException("No mod info found");
            }

            String fileName = filePath.getFileName().toString();

            try (var db = Util.services().getDbConnection()) {
                Integer fileId = db.transactionResult(configuration -> {
                    // discord_users
                    configuration.dsl().insertInto(DISCORD_USERS,
                                    DISCORD_USERS.SNOWFLAKE,
                                    DISCORD_USERS.DISPLAY_NAME
                            )
                            .values(snowflake, userDisplayName)
                            .onDuplicateKeyUpdate()
                            .set(DISCORD_USERS.DISPLAY_NAME, userDisplayName).execute();


                    for (CommonModInfo modInfo : modInfoList) {

                        // mods
                        configuration.dsl().insertInto(MODS,
                                        MODS.ID,
                                        MODS.PROJECT_URL,
                                        MODS.ISSUES_URL
                                )
                                .values(modInfo.id(), modInfo.projectUrl().orElse(null), modInfo.issuesUrl().orElse(null))
                                .onConflictDoNothing().execute();

                        // mod_owners
                        var owner = configuration.dsl().fetchAny(MOD_OWNERS, MOD_OWNERS.MOD_ID.eq(modInfo.id()));

                        // only create owner entry on first upload
                        if (owner == null) {
                            configuration.dsl().insertInto(MOD_OWNERS,
                                            MOD_OWNERS.MOD_ID,
                                            MOD_OWNERS.USER_ID
                                    )
                                    .values(modInfo.id(), discordUser.getId()).execute();
                        }
                    }

                    var rootMod = modInfoList.getFirst();

                    // mod_files
                    var modFilesRecord = configuration.dsl().insertInto(MOD_FILES,
                                    MOD_FILES.MOD_ID,
                                    MOD_FILES.UPLOADER_ID,
                                    MOD_FILES.MOD_VERSION,
                                    MOD_FILES.FILE_NAME,
                                    MOD_FILES.SHA_512
                            )
                            .values(rootMod.id(), snowflake, rootMod.version(), fileName, sha512.byteValue())
                            .onConflictDoNothing().returning(MOD_FILES.ID).fetchOne();
                    Preconditions.checkNotNull(modFilesRecord, "Unable to create record for: " + filePath);

                    //TODO audit log

                    return modFilesRecord.getId();
                });
                return Pair.of(modInfoList.getFirst(), fileId);
            }
        }, Util.BACKGROUND_EXECUTOR);
    }

    public static CompletableFuture<Void> handleApproval(long userId, long messageId, GeneralConfig cfg, MinecraftServerConfig serverCfg) {
        return CompletableFuture.runAsync(() -> {

            var serverPath = serverCfg.path();

            try (var db = Util.services().getDbConnection()) {
                db.transaction(configuration -> {
                    var pendingRecord = configuration.dsl().deleteFrom(APPROVAL_QUEUE)
                            .where(APPROVAL_QUEUE.MESSAGE_ID.eq(messageId))
                            .returning(APPROVAL_QUEUE.MOD_FILE_ID)
                            .fetchOne();

                    if (pendingRecord == null) {
                        // does not exist, do nothing
                        return;
                    }

                    var modFileRecord = configuration.dsl().select(MOD_FILES.MOD_ID)
                            .from(MOD_FILES)
                            .where(MOD_FILES.ID.eq(pendingRecord.getModFileId()))
                            .fetchOne();

                    Preconditions.checkNotNull(modFileRecord, "No database record found for mod file " + pendingRecord.getModFileId());

                    //delete old file(s) if exists and mark inactive
                    var activeFiles = configuration.dsl().update(MOD_FILES)
                            .set(MOD_FILES.ACTIVE, false)
                            .where(MOD_FILES.MOD_ID.eq(modFileRecord.value1()).and(MOD_FILES.ACTIVE.eq(true)))
                            .returning(MOD_FILES.FILE_NAME)
                            .fetch();

                    for (ModFilesRecord file : activeFiles) {
                        var filePath = serverPath.resolve(file.getFileName()).normalize();
                        if (!filePath.startsWith(serverPath.normalize())) {
                            throw new IllegalArgumentException("Invalid file path " + filePath + " for server path " + serverPath);
                        }
                        Files.deleteIfExists(filePath);
                    }

                    // create new file in target location
                    var newFileInfo = configuration.dsl().update(MOD_FILES)
                            .set(MOD_FILES.ACTIVE, true)
                            .where(MOD_FILES.ID.eq(pendingRecord.getModFileId()))
                            .returning(MOD_FILES.FILE_NAME, MOD_FILES.SHA_512)
                            .fetchOne();
                    Preconditions.checkNotNull(newFileInfo, "No database record found for mod file " + pendingRecord.getModFileId());

                    var hash = Hash.fromBytes(Hash.Type.SHA512, newFileInfo.getSha_512());
                    var cacheFileName = DownloadHelper.getCacheFileName(cfg, serverCfg, hash, newFileInfo.getFileName());
                    Preconditions.checkState(Files.exists(cacheFileName), "File not found in cache " + cacheFileName);

                    var targetPath = serverPath.resolve(newFileInfo.getFileName()).normalize();
                    if (!targetPath.startsWith(serverPath.normalize())) {
                        throw new IllegalArgumentException("Invalid file path " + targetPath + " for server path " + serverPath);
                    }
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(cacheFileName, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    //TODO audit log
                });
            }
        }, Util.BACKGROUND_EXECUTOR);
    }
}
