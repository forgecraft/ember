package net.forgecraft.services.ember.app.mods.parser;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Preconditions;
import net.forgecraft.services.ember.app.mods.CommonModInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// https://fabricmc.net/wiki/documentation:fabric_mod_json_spec
@JsonIgnoreProperties(ignoreUnknown = true)
public record FabricModJson(
        int schemaVersion,
        String id,
        String version,
        Optional<String> name,
        Optional<String> description,
        Optional<List<Person>> authors,
        Optional<List<Person>> contributors,
        Optional<ContactInfo> contact,

        // can be String or String[]
//        Optional<String> license,
        Optional<String> icon,
        Optional<String> environment,
        // entrypoints
        // mixins
        // dependencies etc.

        Optional<Map<String, ObjectNode>> custom
) {

    @JsonSerialize(using = Person.Serializer.class)
    @JsonDeserialize(using = Person.Deserializer.class)
    public record Person(String name, Optional<ContactInfo> contact) {

        public static class Serializer extends StdSerializer<Person> {

            public Serializer() {
                this(null);
            }

            public Serializer(Class<Person> t) {
                super(t);
            }

            @Override
            public void serialize(Person value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                if (value.contact().isPresent()) {
                    gen.writeStartObject();
                    gen.writeStringField("name", value.name());
                    gen.writePOJOField("contact", value.contact().get());
                } else {
                    gen.writeString(value.name());
                }
            }
        }

        public static class Deserializer extends StdDeserializer<Person> {

            public Deserializer() {
                this(null);
            }

            public Deserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public Person deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                var data = parser.getCodec().readTree(parser);
                if (data.isValueNode()) {
                    var name = ((ValueNode) data).textValue();
                    return new Person(name, Optional.empty());
                } else {
                    var name = ((ValueNode) data.get("name")).textValue();
                    Optional<ContactInfo> contact;
                    if (data.get("contact") instanceof JsonNode node) {
                        var typeRef = new TypeReference<Optional<ContactInfo>>() {
                        };
                        contact = parser.getCodec().readValue(node.traverse(), typeRef);
                    } else {
                        contact = Optional.empty();
                    }

                    return new Person(name, contact);
                }
            }
        }
    }

    public record ContactInfo(
            Optional<String> email,
            Optional<String> irc,
            Optional<String> homepage,
            Optional<String> issues,
            Optional<String> sources,
            @JsonAnySetter
            Map<String, String> _other
    ) {
    }

    public void validate() {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkNotNull(version, "version must not be null");

        Preconditions.checkState(schemaVersion == 1, "Unsupported schema version " + schemaVersion);
        Preconditions.checkState(id.matches("^[a-z][a-z0-9-_]{1,63}$"), "Invalid mod id " + id);
    }

    public CommonModInfo asCommonModInfo() {
        return new CommonModInfo(CommonModInfo.Type.FABRIC, id(), name().orElse(id()), version(), contact().flatMap(ContactInfo::homepage), contact().flatMap(ContactInfo::issues));
    }
}
