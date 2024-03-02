package net.forgecraft.services.ember.db;

import net.forgecraft.services.ember.db.migration.MigrationHelper;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("data"));
        String dbUrl = "jdbc:sqlite:data/sqlite.db";

        try (var ctx = DSL.using(dbUrl)) {

            // foreign key support in SQLite must be enabled and will be immediately committed,
            // cannot be done in a transaction
            ctx.execute("PRAGMA foreign_keys = ON;");

            MigrationHelper.applyPendingMigrations(ctx);

            ctx.execute("PRAGMA optimize;");
        }
    }
}
