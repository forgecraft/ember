package net.forgecraft.services.ember.db.migration;

import org.jooq.CloseableDSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

public class MigrationHelper {

    public static final String MIGRATIONS_TABLE_NAME = "__migrations";
    public static final Pattern VALID_MIGRATION_NAME = Pattern.compile("\\d{3,}_.+");

    public static void applyPendingMigrations(CloseableDSLContext ctx) {

        // create migrations table programmatically, then apply pending migrations
        ctx.createTableIfNotExists(MIGRATIONS_TABLE_NAME)
                .column("id", SQLDataType.INTEGER.identity(true))
                .column("version", SQLDataType.INTEGER.notNull())
                .column("name", SQLDataType.VARCHAR(255).notNull())
                .constraints(
                        DSL.unique("name"),
                        DSL.unique("version")
                )
                .execute();

        int currentVersion = getCurrentVersion(ctx);

        List<Migration> pendingMigrations;

        try {
            var rs = MigrationHelper.class.getClassLoader().getResource("_donotremovethisisahack");
            if(rs == null) {
                throw new IllegalStateException("unable to find own file path");
            }
            var jarRoot = Path.of(rs.toURI()).getParent();

            pendingMigrations = getPendingMigrations(currentVersion, jarRoot.resolve("migrations"));
            //TODO logging
            System.out.println("Found " + pendingMigrations.size() + " pending migrations.");
        } catch (IOException e) {
            // TODO logging
            throw new UncheckedIOException("unable to apply migrations", e);
        } catch (URISyntaxException e) {
            // should NEVER happen
            throw new RuntimeException(e);
        }

        for (Migration migration : pendingMigrations) {
            try {
                ctx.transaction(trx -> {
                    var up = migration.getUp();
                    if (up == null) {
                        //TODO logging and skip?
                        throw new IllegalStateException("Up migration not found for " + migration.name());
                    }

                    for (String statement : up) {
                        trx.dsl().execute(statement);
                    }

                    trx.dsl()
                            .insertInto(DSL.table(MIGRATIONS_TABLE_NAME), DSL.field("version"), DSL.field("name"))
                            .values(migration.version(), migration.name())
                            .execute();
                });
            } catch (RuntimeException e) { // DataAccessException
                throw new RuntimeException("Failed to apply migration " + migration.name(), e);
            }
        }
    }

    private static List<Migration> getPendingMigrations(int currentVersion, Path migrationsDir) throws IOException {
        var all = parseMigrations(migrationsDir);
        //TODO debug logging
        System.out.println("Found " + all.size() + " migrations.");

        return all.stream().filter(migration -> migration.test(currentVersion)).sorted().toList();
    }

    private static int getCurrentVersion(CloseableDSLContext ctx) {

        var result = ctx.select(DSL.max(DSL.field("version"))).from(MIGRATIONS_TABLE_NAME).fetchOne();

        if (result == null) {
            return -1;
        }

        Integer max = (Integer) result.value1();

        return max != null ? max : -1;
    }

    public static List<Migration> parseMigrations(Path migrationsDir) throws IOException {
        if (!Files.isDirectory(migrationsDir, LinkOption.NOFOLLOW_LINKS)) {
            //TODO logging
            System.err.println("Migrations directory not found at " + migrationsDir.toAbsolutePath());
            return List.of();
        }
        var migrationsDirName = migrationsDir.getFileName().toString();

        List<Migration> migrations = new ArrayList<>();
        Files.walkFileTree(migrationsDir, EnumSet.noneOf(FileVisitOption.class), 2, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                var dirName = dir.getFileName().toString();

                // skip the root dir
                if(dirName.equals(migrationsDirName)) {
                    return FileVisitResult.CONTINUE;
                }


                if (!VALID_MIGRATION_NAME.asMatchPredicate().test(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                try {
                    var migrationVersion = Integer.parseInt(dirName.substring(0, dirName.indexOf('_')), 10);
                    migrations.add(new Migration(dirName, migrationVersion, dir));
                } catch (NumberFormatException e) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }
        });

        Collections.sort(migrations);
        return migrations;
    }
}
