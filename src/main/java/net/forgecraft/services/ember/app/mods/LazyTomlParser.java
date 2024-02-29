package net.forgecraft.services.ember.app.mods;

/**
 * I don't want to rely on a third-party library to parse TOML files, so I'm going to write something very lazy
 * that will try it's best to get some of the information out of the file without properly parsing it.
 */
public class LazyTomlParser {
    private final String toml;
    private String cachedModsSection = null;

    public LazyTomlParser(String toml) {
        this.toml = toml;
    }

    public CommonModInfo parse() {
        var id = extractValue("modId");
        var name = extractValue("displayName");
        var version = extractValue("version");

        return new CommonModInfo(id, name, version);
    }

    /**
     * Extract a value from the TOML file
     * This will always pull the first value it finds, so it's not very reliable...
     *
     * @param key The key to extract
     * @return The value
     */
    private String extractValue(String key) {
        // Seek the mod specific area. This is denoted by the [[mods]] section and ends at the next [[ section ]]
        if (cachedModsSection == null) {
            var modsSectionStart = toml.indexOf("[[mods]]");
            if (modsSectionStart == -1) {
                return "";
            }

            var modsSectionEnd = toml.indexOf("[[", modsSectionStart + 1);
            if (modsSectionEnd == -1) {
                return "";
            }

            cachedModsSection = toml.substring(modsSectionStart, modsSectionEnd);
        }

        var keyIndex = cachedModsSection.indexOf(key);
        if (keyIndex == -1) {
            return "";
        }

        var valueIndex = cachedModsSection.indexOf("=", keyIndex);
        if (valueIndex == -1) {
            return "";
        }

        var startQuoteIndex = cachedModsSection.indexOf("\"", valueIndex);
        if (startQuoteIndex == -1) {
            return "";
        }

        var endQuoteIndex = cachedModsSection.indexOf("\"", startQuoteIndex + 1);
        if (endQuoteIndex == -1) {
            return "";
        }

        return cachedModsSection.substring(startQuoteIndex + 1, endQuoteIndex);
    }
}
