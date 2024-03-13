package net.forgecraft.services.ember.util.serialization;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class StatusCodeOnlyBodyHandlerApache implements HttpClientResponseHandler<Integer> {

    public static final StatusCodeOnlyBodyHandlerApache INSTANCE = new StatusCodeOnlyBodyHandlerApache();

    private StatusCodeOnlyBodyHandlerApache() {
    }

    @Override
    public Integer handleResponse(ClassicHttpResponse response) {
        return response.getCode();
    }
}
