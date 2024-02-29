package net.forgecraft.services.ember;


import net.forgecraft.services.ember.app.Services;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static Main INSTANCE;

    private final Services services;
    private final DiscordApi discordApi;

    public static void main(String[] args) {
        LOGGER.info("Starting application...");
        INSTANCE = new Main(args);
    }

    // Real application start
    public Main(String[] args) {
        this.services = new Services(args);
        this.discordApi = new DiscordApiBuilder()
                .setToken(services.getConfig().getDiscord().token())
                // Pulled over from the old bot, not sure if all of these are needed
                .addIntents(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.GUILD_MESSAGE_REACTIONS, Intent.DIRECT_MESSAGES, Intent.DIRECT_MESSAGE_REACTIONS)
                .login()
                .join();

        this.discordApi.addMessageCreateListener(event -> {
            if (event.getMessageContent().equalsIgnoreCase("!ping")) {
                event.getChannel().sendMessage("Pong!");
            }
        });
    }

    public Services services() {
        return services;
    }

    public DiscordApi discordApi() {
        return discordApi;
    }
}
