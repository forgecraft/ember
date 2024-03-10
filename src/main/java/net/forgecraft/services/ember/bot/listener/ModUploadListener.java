package net.forgecraft.services.ember.bot.listener;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.forgecraft.services.ember.app.config.Config;
import net.forgecraft.services.ember.app.config.GeneralConfig;
import net.forgecraft.services.ember.app.config.MinecraftServerConfig;
import net.forgecraft.services.ember.app.mods.ModFileManager;
import net.forgecraft.services.ember.app.mods.downloader.DownloadHelper;
import net.forgecraft.services.ember.app.mods.downloader.DownloadInfo;
import net.forgecraft.services.ember.app.mods.downloader.DownloaderFactory;
import net.forgecraft.services.ember.util.Util;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ModUploadListener implements MessageCreateListener {

    private static final String STATUS_SUCCESS = "✅";
    private static final String STATUS_PROCESSING = "⌛";
    private static final String STATUS_INVALID_MESSAGE = "❌";
    private static final String STATUS_ERROR = "⚠";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModUploadListener.class);
    private final GeneralConfig cfg;
    private final Long2ObjectMap<MinecraftServerConfig> minecraftServers = new Long2ObjectArrayMap<>();

    public ModUploadListener(Config cfg) {
        this.cfg = cfg.getGeneral();
        for (MinecraftServerConfig server : cfg.getMinecraftServers()) {
            minecraftServers.put(server.uploadChannel(), server);
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        var msg = event.getMessage();

        // only listen to messages sent by users
        if (msg.getType() != MessageType.NORMAL) {
            return;
        }

        var serverCfg = minecraftServers.get(msg.getChannel().getId());
        if (serverCfg == null) {
            // don't know this channel, ignore message
            return;
        }

        msg.addReaction(STATUS_PROCESSING);

        // analyze the message and search for download targets
        CompletableFuture.runAsync(() -> {

            List<DownloadInfo> downloads = new ArrayList<>();

            // simplest case first: are there file attachments?
            for (MessageAttachment attachment : event.getMessage().getAttachments()) {
                if (attachment.getFileName().endsWith(".jar")) {
                    var download = DownloaderFactory.INSTANCE.tryDownload(attachment.getUrl().toString());

                    if (download != null) {
                        downloads.add(download);
                    }
                }
            }

            // next: test each line for being a valid download target
            if (!msg.getContent().isEmpty()) {
                msg.getContent().lines()
                        // split on whitespace in case there are multiple targets on one line
                        .flatMap(line -> Arrays.stream(line.split("\\s")))
                        .filter(Predicate.not(String::isBlank))
                        // surrounding a URL with <> causes it to not embed, so we special-case this to make it still parse as valid URL later
                        .map(s -> s.replaceAll("^<(.*)>$", "$1"))
                        .map(DownloaderFactory.INSTANCE::tryDownload)
                        .filter(Objects::nonNull)
                        .forEach(downloads::add);
            }

            if (downloads.isEmpty()) {
                // unable to find any download target, cancel further processing
                msg.removeReactionByEmoji(STATUS_PROCESSING);
                msg.addReaction(STATUS_INVALID_MESSAGE);
                return;
            }

            var uploader = msg.getAuthor().asUser().orElseThrow();

            AtomicBoolean errored = new AtomicBoolean(false);

            CompletableFuture.allOf(downloads.stream().map(download -> download.getFileContents()
                            .thenApplyAsync(bytes -> {
                                var hash = Objects.requireNonNull(download.getSha512());
                                var hashString = hash.stringValue();
                                try {
                                    var target = serverCfg.getNameAsPath(cfg.storageDir()).resolve(hashString).resolve(download.getFileName());
                                    DownloadHelper.saveTo(target, bytes);
                                    return Pair.of(hash, target);
                                } catch (IOException e) {
                                    throw new UncheckedIOException("error saving file", e);
                                }
                            }, Util.BACKGROUND_EXECUTOR)
                            .thenCompose(data -> ModFileManager.handleDownload(data.left(), data.right(), uploader, serverCfg))
                            .exceptionally(ex -> {
                                LOGGER.error("Download error on {}", download.getUrl(), ex);
                                errored.set(true);
                                msg.addReaction(STATUS_ERROR);
                                return null;
                            })).toArray(CompletableFuture[]::new))
                    .thenRun(() -> {
                        msg.removeReactionByEmoji(STATUS_PROCESSING);
                        if (!errored.get()) {
                            msg.addReaction(STATUS_SUCCESS);
                        }
                    });
        }, Util.BACKGROUND_EXECUTOR);
    }
}
