package net.forgecraft.services.ember.db.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

public final class Migration implements IntPredicate, Comparable<Migration> {

    private final String name;
    private final int version;
    private final Path migrationPath;

    public Migration(String name, int version, Path migrationPath) {
        this.name = name;
        this.version = version;
        this.migrationPath = migrationPath;
    }

    @Override
    public boolean test(int value) {
        return version() > value;
    }

    @Override
    public int compareTo(@NotNull Migration o) {
        return Comparator.comparingInt(Migration::version).compare(this, o);
    }

    public String name() {
        return name;
    }

    public int version() {
        return version;
    }

    @Nullable
    public List<String> getUp() throws IOException {
        return resolve(Type.UP);
    }

    @Nullable
    public List<String> getDown() throws IOException {
        return resolve(Type.DOWN);
    }

    @Nullable
    private List<String> resolve(Type type) throws IOException {
        var filePath = this.migrationPath.resolve(type.getFileName());
        if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
            // strip out comments and empty lines
            var fileContent = Files.readAllLines(filePath).stream().map(s -> s.replaceAll("--.+", "")).filter(s -> !s.isBlank()).toList();

            // manually split statements because SQLite JDBC driver does not support multiple statements in a single execute
            List<String> statements = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (String line : fileContent) {
                int idx = line.indexOf(';');
                while (idx != -1) {
                    sb.append(line, 0, idx).append(';');
                    statements.add(sb.toString());
                    sb.setLength(0);

                    line = line.substring(idx + 1);
                    idx = line.indexOf(';');
                }

                sb.append(line);
                sb.append('\n');
            }
            return List.copyOf(statements);
        }
        return null;
    }

    public enum Type {
        UP("up.sql"),
        DOWN("down.sql");

        private final String fileName;

        Type(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
