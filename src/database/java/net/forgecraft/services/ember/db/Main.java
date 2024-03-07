package net.forgecraft.services.ember.db;

import org.jooq.impl.DSL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("data"));
        String dbUrl = "jdbc:sqlite:data/sqlite.db";

        try (var ctx = DSL.using(dbUrl)) {
            DatabaseManager.bootstrapDatabase(ctx);
        }
    }
}
