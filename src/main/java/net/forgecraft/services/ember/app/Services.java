package net.forgecraft.services.ember.app;

import net.forgecraft.services.ember.app.config.Config;
import org.jooq.CloseableDSLContext;
import org.jooq.impl.DSL;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Basic services class to use as dependency injection
 */
public class Services {
    private final Config config;
    private final Supplier<CloseableDSLContext> database;

    public Services(Path configPath) {
        this.config = Config.load(configPath);
        var dbUrl = "jdbc:sqlite:%s".formatted(config.getGeneral().databasePath());
        this.database = () -> DSL.using(dbUrl);
    }

    public Config getConfig() {
        return config;
    }

    public CloseableDSLContext getDbConnection() {
        return database.get();
    }
}
