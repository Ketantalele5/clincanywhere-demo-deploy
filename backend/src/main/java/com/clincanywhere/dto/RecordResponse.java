package com.clincanywhere.dto;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;

/** Flat response DTO preserves the existing frontend API contract. */
public record RecordResponse(@JsonAnyGetter Map<String, Object> fields) { }
