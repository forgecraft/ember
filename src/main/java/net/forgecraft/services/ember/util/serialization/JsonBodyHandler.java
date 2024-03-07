package net.forgecraft.services.ember.util.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.Function;

public class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {

    private final Class<T> type;
    private final ObjectMapper mapper;

    private JsonBodyHandler(Class<T> type, ObjectMapper mapper) {
        this.type = type;
        this.mapper = mapper;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        if (responseInfo.statusCode() != 200) {
            return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofByteArray(), bytes -> null);
        }

        return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofInputStream(), this.asJson());
    }

    private Function<InputStream, T> asJson() {
        return inputStream -> {
            try {
                return mapper.readValue(inputStream, type);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        };
    }

    public static <T> JsonBodyHandler<T> of(Class<T> type, ObjectMapper mapper) {
        return new JsonBodyHandler<>(type, mapper);
    }

    public static <T> JsonBodyHandler<T> of(Class<T> type) {
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
