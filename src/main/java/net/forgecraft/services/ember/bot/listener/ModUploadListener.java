package net.forgecraft.services.ember.bot.listener;

import net.forgecraft.services.ember.app.Config;
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
import java.nio.file.Path;
import java.util.ArrayList;
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
    private final Config.DiscordConfig cfg;

    public ModUploadListener(Config.DiscordConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        // not our channel, ignore message
        if (event.getChannel().getId() != cfg.uploadChannel()) {
            return;
        }

        var msg = event.getMessage();

        // only listen to messages sent by users
        if (msg.getType() != MessageType.NORMAL) {
            return;
        }

        msg.addReaction(STATUS_PROCESSING);

        // analyze the message and search for download targets
        CompletableFuture.runAsync(() -> {

            // TODO implement other cases
            // TODO handle multiple cases per message

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
                        .filter(Predicate.not(String::isBlank))
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

            //TODO save downloads to global dir and DB

            AtomicBoolean errored = new AtomicBoolean(false);

            CompletableFuture.allOf(downloads.stream().map(download -> download.getFileContents().thenAcceptAsync(bytes -> {
                        var hash = Objects.requireNonNull(download.getSha512()).toString();
                        var target = Path.of(".").resolve(hash).resolve(download.getFileName());
                        try {
                            DownloadHelper.saveTo(target, bytes);
                        } catch (IOException e) {
                            LOGGER.error("error saving file: {}", target, e);
                        }
                    }).exceptionally(ex -> {
                        LOGGER.error("Download error on {}", download.getUrl(), ex);
                        errored.set(true);
                        msg.addReaction(STATUS_ERROR);
                        return null;
                    })).toArray(CompletableFuture[]::new))
                    .thenComposeAsync(aVoid -> msg.removeReactionByEmoji(STATUS_PROCESSING))
                    .thenRunAsync(() -> {
                        if (!errored.get()) {
                            msg.addReaction(STATUS_SUCCESS);
                        }
                    });
        }, Util.BACKGROUND_EXECUTOR);
    }
}
