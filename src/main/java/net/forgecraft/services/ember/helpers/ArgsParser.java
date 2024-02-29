package net.forgecraft.services.ember.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses command line arguments.
 * I wrote this before seeing apache commons cli, so I would probably use that instead if I were to write this again.
 */
public class ArgsParser {
    private final Map<String, String> args;

    private ArgsParser(String[] args) {
        this.args = this.parseArgs(args);
    }

    public static ArgsParser parse(String[] args) {
        return new ArgsParser(args);
    }

    public String get(String key) {
        return args.get(key);
    }

    public String getOrThrow(String key) {
        String value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Argument " + key + " is required");
        }
        return value;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    /**
     * Relative simple parser that creates a key-value map from the command line arguments.
     * Example input: --debug --test=123 --test2 value
     * @param args The command line arguments
     * @return A map of key-value pairs
     */
    public Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsedArguments = new HashMap<>();

        String currentKey = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                // Argument is of the form "--flag" or "--flag=value"
                int equalsIndex = arg.indexOf('=');
                if (equalsIndex != -1) {
                    // Argument is of the form "--flag=value"
                    currentKey = arg.substring(2, equalsIndex);
                    String value = arg.substring(equalsIndex + 1);
                    parsedArguments.put(currentKey, value);
                } else {
                    // Argument is of the form "--flag"
                    currentKey = arg.substring(2);
                    parsedArguments.put(currentKey, null);
                }
            } else if (currentKey != null) {
                // Argument is of the form "--flag value"
                parsedArguments.put(currentKey, arg);
                currentKey = null;
            }
        }

        return parsedArguments;
    }
}
