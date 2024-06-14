package net.forgecraft.services.ember.util;

import java.net.URI;
import java.util.Objects;

public class ProjectInfo {

    private static final String NAME = "Ember";
    private final String version = Objects.requireNonNullElse(getClass().getPackage().getImplementationVersion(), "development");
    private final URI url = URI.create("https://github.com/forgecraft/Ember");

    private String userAgentString;

    public static final ProjectInfo INSTANCE = new ProjectInfo();

    private ProjectInfo() {
    }

    public String getName() {
        return NAME;
    }

    public String getVersion() {
        return version;
    }

    public URI getUrl() {
        return url;
    }

    /**
     * @return a <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent">User-Agent</a> string
     */
    public String asUserAgentString() {
        if(userAgentString == null) {
            userAgentString = "%s/%s (%s)".formatted(getName(), getVersion(), getUrl());
        }

        return userAgentString;
    }
}
