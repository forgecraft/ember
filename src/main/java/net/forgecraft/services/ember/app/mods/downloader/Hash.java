package net.forgecraft.services.ember.app.mods.downloader;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public record Hash(Type type, HashCode value) {

    public enum Type {
        SHA512("sha512"),
        SHA256("sha256"),
        @Deprecated
        SHA1("sha1"),
        @Deprecated
        MD5("md5");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Hash fromString(Type type, String hash) {
        return new Hash(type, HashCode.fromString(hash));
    }

    @SuppressWarnings("deprecation")
    public static Hash fromBytes(Type type, byte[] data) {
        HashFunction function = switch (type) {
            case SHA512 -> Hashing.sha512();
            case SHA256 -> Hashing.sha256();
            case SHA1 -> Hashing.sha1();
            case MD5 -> Hashing.md5();
        };
        return new Hash(type, function.hashBytes(data));
    }

    public String toString() {
        return type() + ":" + value().toString();
    }

    public String stringValue() {
        return value().toString();
    }

    public byte[] byteValue() {
        return value().asBytes();
    }
}
