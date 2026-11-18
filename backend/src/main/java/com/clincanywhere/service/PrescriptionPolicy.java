package com.clincanywhere.service;

import java.util.Map;

/** Prescription-specific rules used by the generic care-record use case. */
public interface PrescriptionPolicy {
    Map<String, Object> prepareForWrite(String category, boolean doctor, String actor, Map<String, Object> fields);
    void requireUpdatable(String category, boolean doctor, Map<String, Object> existingFields,
                          Map<String, Object> requestedFields);
    void requireDeletable(String category, boolean doctor, Map<String, Object> existingFields);
}
