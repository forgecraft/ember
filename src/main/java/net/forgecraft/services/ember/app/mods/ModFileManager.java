package net.forgecraft.services.ember.app.mods;

import com.google.common.base.Preconditions;
import net.forgecraft.services.ember.app.config.MinecraftServerConfig;
import net.forgecraft.services.ember.app.mods.downloader.Hash;
import net.forgecraft.services.ember.app.mods.parser.CommonModInfo;
import net.forgecraft.services.ember.app.mods.parser.ModInfoParser;
import net.forgecraft.services.ember.util.Util;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.forgecraft.services.ember.db.schema.Tables.*;

public class ModFileManager {

    public static CompletableFuture<Void> handleDownload(Hash sha512, Path filePath, User discordUser, MinecraftServerConfig serverConfig) {
        return recordDownload(sha512, filePath, discordUser).thenAccept(modInfo -> {
            //TODO copy new file and delete old file
        });
    }

    public static CompletableFuture<CommonModInfo> recordDownload(Hash sha512, Path filePath, User discordUser) {
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
                db.transaction(configuration -> {
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
                    configuration.dsl().insertInto(MOD_FILES,
                                    MOD_FILES.MOD_ID,
                                    MOD_FILES.UPLOADER_ID,
                                    MOD_FILES.MOD_VERSION,
                                    MOD_FILES.FILE_NAME,
                                    MOD_FILES.SHA_512
                            )
                            .values(rootMod.id(), snowflake, rootMod.version(), fileName, sha512.byteValue())
                            .onConflictDoNothing().execute();

                    //TODO audit log
                });
            }
            return modInfoList.getFirst();
        }, Util.BACKGROUND_EXECUTOR);
    }
}
