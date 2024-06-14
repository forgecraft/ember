package net.forgecraft.services.ember.util.serialization;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class StatusCodeOnlyResponseHandler implements HttpClientResponseHandler<Integer> {

    public static final StatusCodeOnlyResponseHandler INSTANCE = new StatusCodeOnlyResponseHandler();

    private StatusCodeOnlyResponseHandler() {
    }

    @Override
    public Integer handleResponse(ClassicHttpResponse response) {
        return response.getCode();
    }
}
