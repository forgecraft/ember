package net.forgecraft.services.ember.app.config;

public record DiscordConfig(
        String token,
        long guild,
        long[] adminRoles
) {
    public static DiscordConfig create() {
        return new DiscordConfig("YOUR_DISCORD_TOKEN", -1, new long[0]);
    }
}
