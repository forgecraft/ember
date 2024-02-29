package net.forgecraft.services.ember.app.mods;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Takes in a mod of any form and attempts to parse it's metadata by either reading it's mods.toml file or from it's
 * fabric.mod.json file.
 *
 * // TODO: Maybe have special support for server plugins?
 */
public class ModInfoParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModInfoParser.class);

    private final Path modPath;

    public ModInfoParser(Path modPath) {
        this.modPath = modPath;
    }

    /**
     * Attempt to parse the mod's metadata
     *
     * @return The mod's metadata
     */
    public CommonModInfo parse() throws IOException, RuntimeException {
        var seekInfoFile = this.seekInfoFile();
        if (seekInfoFile == null) {
            throw new RuntimeException("The mod does not contain a mods.toml or fabric.mod.json file");
        }

        if (seekInfoFile[0].equals("mods.toml")) {
            return this.parseModsToml(seekInfoFile[1]);
        } else if (seekInfoFile[0].equals("fabric.mod.json")) {
            return this.parseFabricModJson(seekInfoFile[1]);
        }

        throw new RuntimeException("Unknown mod metadata file: " + seekInfoFile[0]);
    }

    private CommonModInfo parseModsToml(String fileContents) throws RuntimeException {
        var parser = new LazyTomlParser(fileContents);

        CommonModInfo result = parser.parse();
        // Test for the required fields
        if (result.id().isEmpty() || result.name().isEmpty() || result.version().isEmpty()) {
            throw new RuntimeException("The mods.toml file is missing required fields");
        }

        return result;
    }

    private CommonModInfo parseFabricModJson(String fileContents) throws RuntimeException, IOException {
        var mapper = new ObjectMapper();
        var data = mapper.readValue(fileContents, Map.class);

        if (!data.containsKey("id") || !data.containsKey("name") || !data.containsKey("version")) {
            throw new RuntimeException("The fabric.mod.json file is missing required fields");
        }

        // The unsafe nature here isn't great, but it's the best we can do without a proper schema, and I don't want to
        // make one myself.
        var modId = (String) data.get("id");
        var modName = (String) data.get("name");
        var modVersion = (String) data.get("version");

        return new CommonModInfo(modId, modName, modVersion);
    }

    @Nullable
    private String[] seekInfoFile() throws IOException {
        // Create a file system for the jar
        try (FileSystem system  = FileSystems.newFileSystem(modPath)) {
            // Seek a mods.toml file or a fabric.mod.json file
            var modsToml = system.getPath("META-INF/mods.toml");
            if (Files.exists(modsToml)) {
                return new String[]{"mods.toml", Files.readString(modsToml)};
            }

            var fabricModJson = system.getPath("fabric.mod.json");
            if (Files.exists(fabricModJson)) {
                return new String[]{"fabric.mod.json", Files.readString(fabricModJson)};
            }
        }

        return null;
    }
}
