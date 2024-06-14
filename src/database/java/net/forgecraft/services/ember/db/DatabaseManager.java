package net.forgecraft.services.ember.db;

import net.forgecraft.services.ember.db.migration.MigrationHelper;
import org.jooq.CloseableDSLContext;

public class DatabaseManager {

    public static void bootstrapDatabase(CloseableDSLContext ctx) {

        // foreign key support in SQLite must be enabled and will be immediately committed,
        // cannot be done in a transaction
        ctx.execute("PRAGMA foreign_keys = ON;");

        MigrationHelper.applyPendingMigrations(ctx);

        ctx.execute("PRAGMA optimize;");
    }
}
