package com.majstornaklik.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    public static String toJson(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list != null ? list : List.of());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Greška pri serializaciji JSON-a", e);
        }
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
