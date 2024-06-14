package net.forgecraft.services.ember.bot.listener;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.forgecraft.services.ember.app.config.Config;
import net.forgecraft.services.ember.app.config.GeneralConfig;
import net.forgecraft.services.ember.app.config.MinecraftServerConfig;
import net.forgecraft.services.ember.app.mods.ModFileManager;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModApprovalListener implements ReactionAddListener {

    private static final List<String> APPROVAL_REACTION = List.of("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿");

    private static final Logger LOGGER = LoggerFactory.getLogger(ModApprovalListener.class);

    private final GeneralConfig cfg;
    private final LongSet adminRoles;
    private final Long2ObjectMap<MinecraftServerConfig> minecraftServers = new Long2ObjectArrayMap<>();

    public ModApprovalListener(Config config) {
        this.cfg = config.getGeneral();
        this.adminRoles = LongSet.of(config.getDiscord().adminRoles());

        for (MinecraftServerConfig server : config.getMinecraftServers()) {
            minecraftServers.put(server.uploadChannel(), server);
        }
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        var reaction = event.requestReaction().join().orElse(null);
        if (reaction == null) {
            // reaction removed before we got to process it
            return;
        }

        var msg = reaction.getMessage();

        if (!msg.isServerMessage() || msg.getType() != MessageType.NORMAL) {
            return;
        }
        var server = msg.getServer().orElseThrow();

        var emoji = reaction.getEmoji();

        if (!emoji.asUnicodeEmoji().map(APPROVAL_REACTION::contains).orElse(false)) {
            // not an approval reaction
            return;
        }

        var serverCfg = minecraftServers.get(msg.getChannel().getId());
        if (serverCfg == null) {
            // don't know this channel, ignore message
            return;
        }

        var user = event.requestUser().join();
        var isAdmin = user.getRoles(server).stream().anyMatch(role -> adminRoles.contains(role.getId()));
        if (isAdmin) {
            LOGGER.debug("Handling approval for message {} by user {}", msg.getId(), user.getId());
            ModFileManager.handleApproval(user.getId(), msg.getId(), cfg, serverCfg)
                    .exceptionally(ex -> {
                        LOGGER.error("Failed to handle approval", ex);
                        return null;
                    });
        }
    }
}
