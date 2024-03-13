package net.forgecraft.services.ember.util.serialization;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;

public class JsonResponseHandler<T> extends AbstractHttpClientResponseHandler<T> {

    private final Class<T> type;
    private final ObjectMapper mapper;

    private JsonResponseHandler(Class<T> type, ObjectMapper mapper) {
        this.type = type;
        this.mapper = mapper;
    }

    @Override
    public T handleEntity(HttpEntity entity) throws IOException {
        try (var stream = entity.getContent()) {
            return mapper.readValue(stream, type);
        } catch (StreamReadException | DatabindException ex) {
            throw new IOException(ex);
        }
    }

    public static <T> JsonResponseHandler<T> of(Class<T> type, ObjectMapper mapper) {
        return new JsonResponseHandler<>(type, mapper);
    }

    public static <T> JsonResponseHandler<T> of(Class<T> type) {
        return of(type, JsonMapper.builder()
                .findAndAddModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build()
        );
    }
}
