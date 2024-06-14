package net.forgecraft.services.ember.app.mods.parser;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.forgecraft.services.ember.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Takes in a mod of any form and attempts to parse it's metadata by either reading it's mods.toml file or from it's
 * *mod.json file.
 * <p>
 * // TODO: Maybe have special support for server plugins?
 */
public class ModInfoParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModInfoParser.class);

    public ModInfoParser() {
    }

    /**
     * Attempt to parse the mod's metadata
     *
     * @return The mod's metadata
     */
    public static List<CommonModInfo> parse(Path modPath) throws IOException {

        // Create a file system for the jar
        try (FileSystem system = FileSystems.newFileSystem(modPath)) {
            var modsToml = system.getPath("META-INF/mods.toml");
            if (Files.exists(modsToml)) {
                try (var reader = Files.newBufferedReader(modsToml)) {
                    return parseModsToml(reader);
                }
            }
            // NeoForge: neoforge.mods.toml
            modsToml = system.getPath("META-INF/neoforge.mods.toml");
            if (Files.exists(modsToml)) {
                try (var reader = Files.newBufferedReader(modsToml)) {
                    return parseModsToml(reader);
                }
            }

            var quiltModJson = system.getPath("quilt.mod.json");
            if (Files.exists(quiltModJson)) {
                LOGGER.error("Quilt mod.json found, but not supported yet");

                //TODO parse QMJ
            }

            var fabricModJson = system.getPath("fabric.mod.json");
            if (Files.exists(fabricModJson)) {
                try (var reader = Files.newBufferedReader(fabricModJson)) {
                    return parseFabricModJson(reader);
                }
            }
        }

        return List.of();
    }

    private static List<CommonModInfo> parseModsToml(Reader reader) throws RuntimeException {
        var builder = ImmutableList.<CommonModInfo>builder();

        //TODO parse LexForge mods, right now this assumes every mods.toml is a valid NeoForge mod

        CommentedConfig cfgRoot = Util.TOML_PARSER.parse(reader);

        // global properties
        Optional<String> issueTrackerUrl = Optional.ofNullable(cfgRoot.get("issueTrackerURL"));


        List<CommentedConfig> mods = cfgRoot.get("mods");
        if (mods != null) {
            for (CommentedConfig mod : mods) {
                String modId = mod.get("modId"); // on older versions, this is the only mandatory field
                Preconditions.checkNotNull(modId, "Mod id is missing");
                Preconditions.checkState(modId.matches("^[a-z][a-z0-9_]{1,63}$"), "Invalid mod id " + modId);

                String modVersion = mod.get("version");
                if (modVersion == null) {
                    modVersion = "1";
                }

                String displayName = mod.get("displayName");
                if (displayName == null) {
                    displayName = modId;
                }

                Optional<String> modUrl = Optional.ofNullable(mod.get("modUrl"));
                Optional<String> displayUrl = Optional.ofNullable(mod.get("displayURL"));

                builder.add(new CommonModInfo(CommonModInfo.Type.NEOFORGE, modId, displayName, modVersion, modUrl.isPresent() ? modUrl : displayUrl, issueTrackerUrl));
            }
        }

        return builder.build();
    }

    private static List<CommonModInfo> parseFabricModJson(Reader reader) throws RuntimeException, IOException {
        var builder = ImmutableList.<CommonModInfo>builder();

        var data = Util.JACKSON_MAPPER.readTree(reader);

        if (data.isArray()) {
            var typeRef = new TypeReference<List<FabricModJson>>() {
            };
            var list = Util.JACKSON_MAPPER.treeToValue(data, typeRef);
            list.stream().peek(FabricModJson::validate).map(FabricModJson::asCommonModInfo).forEachOrdered(builder::add);
        } else {
            var fmj = Util.JACKSON_MAPPER.treeToValue(data, FabricModJson.class);
            fmj.validate();
            builder.add(fmj.asCommonModInfo());
        }

        return builder.build();
    }
}
