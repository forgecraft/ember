package net.forgecraft.services.ember.util.serialization;

import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;

public class ByteArrayResponseHandler extends AbstractHttpClientResponseHandler<byte[]> {

    public static final ByteArrayResponseHandler INSTANCE = new ByteArrayResponseHandler();

    private ByteArrayResponseHandler() {
    }

    @Override
    public byte[] handleEntity(HttpEntity entity) throws IOException {
        try (var stream = entity.getContent()) {
            return stream.readAllBytes();
        }
    }
}
