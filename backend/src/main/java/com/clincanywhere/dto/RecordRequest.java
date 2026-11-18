package com.clincanywhere.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotEmpty;

/** Keeps the legacy category-specific JSON fields flat while giving the API a request DTO. */
public class RecordRequest {
    @NotEmpty
    private final Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addField(String name, Object value) { fields.put(name, value); }

    @JsonAnyGetter
    public Map<String, Object> fields() {
        // Map.copyOf rejects JSON null values, which would turn an otherwise
        // valid request into an unhandled NullPointerException before service validation.
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}
