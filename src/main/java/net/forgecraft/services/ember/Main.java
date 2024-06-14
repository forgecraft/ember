package net.forgecraft.services.ember;


import net.forgecraft.services.ember.app.Services;
import net.forgecraft.services.ember.bot.listener.ModApprovalListener;
import net.forgecraft.services.ember.bot.listener.ModUploadListener;
import net.forgecraft.services.ember.db.DatabaseManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Deprecated
    public static volatile Main INSTANCE;

    private final Services services;
    private final DiscordApi discordApi;

    public static void main(String[] args) {
        LOGGER.info("Starting application...");

        var opts = CommandLine.populateSpec(Main.Cli.class, args);
        if (opts.helpRequested) {
            CommandLine.usage(opts, System.out);
            return;
        }

        INSTANCE = new Main(opts);
    }

    // Real application start
    public Main(Cli opts) {
        this.services = new Services(opts.configPath);

        try {
            var dbPath = services.getConfig().getGeneral().databasePath();
            Files.createDirectories(dbPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        try (var ctx = services.getDbConnection()) {
            DatabaseManager.bootstrapDatabase(ctx);
        }
        this.discordApi = new DiscordApiBuilder()
                .setToken(services.getConfig().getDiscord().token())
                .addIntents(
                        Intent.GUILDS,
                        Intent.GUILD_MESSAGES, // general message events
                        Intent.MESSAGE_CONTENT, // read message contents for mod uploads
                        Intent.GUILD_MESSAGE_REACTIONS, // read reactions for mod approvals
                        Intent.GUILD_MEMBERS // read member list for admin role checks
                )
                .login()
                .join();

        this.discordApi.addMessageCreateListener(event -> {
            if (event.getMessageContent().equalsIgnoreCase("!ping")) {
                event.getChannel().sendMessage("Pong!");
            }
        });

        this.discordApi.addMessageCreateListener(new ModUploadListener(services.getConfig()));
        this.discordApi.addReactionAddListener(new ModApprovalListener(services.getConfig()));
    }

    public Services services() {
        return services;
    }

    public DiscordApi discordApi() {
        return discordApi;
    }

    public static class Cli {

        @CommandLine.Option(names = {"-c", "--config"}, required = true, description = "Path to the configuration file")
        public Path configPath;

        @CommandLine.Option(names = {"-h", "-?", "--help"}, description = "Prints this help message and exits", usageHelp = true)
        public boolean helpRequested = false;
    }
}
