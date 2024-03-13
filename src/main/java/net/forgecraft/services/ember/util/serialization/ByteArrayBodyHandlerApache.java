package net.forgecraft.services.ember.util.serialization;

import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;

public class ByteArrayBodyHandlerApache extends AbstractHttpClientResponseHandler<byte[]> {

    public static final ByteArrayBodyHandlerApache INSTANCE = new ByteArrayBodyHandlerApache();

    private ByteArrayBodyHandlerApache() {
    }

    @Override
    public byte[] handleEntity(HttpEntity entity) throws IOException {
        try (var stream = entity.getContent()) {
            return stream.readAllBytes();
        }
    }
}
