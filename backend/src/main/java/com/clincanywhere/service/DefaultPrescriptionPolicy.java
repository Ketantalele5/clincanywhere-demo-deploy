package com.clincanywhere.service;

import com.clincanywhere.common.exception.BadRequestException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrescriptionPolicy implements PrescriptionPolicy {
    @Override
    public Map<String, Object> prepareForWrite(String category, boolean doctor, String actor, Map<String, Object> fields) {
        if (fields == null) throw new BadRequestException("Record payload cannot be empty");
        var prepared = new LinkedHashMap<>(fields);
        if ("prescriptions".equals(category)) {
            boolean finalized = "Finalized".equalsIgnoreCase(String.valueOf(prepared.get("clinicalStatus")));
            if (finalized && !doctor) {
                throw new AccessDeniedException("Only a doctor can finalize a prescription");
            }
            if (doctor && finalized) {
                prepared.put("prescribedBy", actor);
                prepared.put("finalizedAt", Instant.now().toString());
            }
        }
        return prepared;
    }

    public void requireUpdatable(String category, boolean doctor, Map<String, Object> existingFields,
                                 Map<String, Object> requestedFields) {
        if (!"prescriptions".equals(category)) return;
        boolean wasFinalized = "Finalized".equalsIgnoreCase(String.valueOf(existingFields.get("clinicalStatus")));
        boolean remainsFinalized = "Finalized".equalsIgnoreCase(String.valueOf(requestedFields.get("clinicalStatus")));
        if (wasFinalized && doctor && !remainsFinalized) {
            throw new AccessDeniedException("A finalized prescription cannot return to draft");
        }
        if (wasFinalized && !doctor && !sameExceptDispenseState(existingFields, requestedFields)) {
            throw new AccessDeniedException("Only the pharmacy dispensing workflow can update a finalized prescription");
        }
        if (wasFinalized && doctor && changesClinicalContent(existingFields, requestedFields)) {
            throw new AccessDeniedException("Finalized prescriptions are immutable; create a corrected prescription instead");
        }
    }

    private boolean sameExceptDispenseState(Map<String, Object> existing, Map<String, Object> requested) {
        var before = new LinkedHashMap<>(existing);
        var after = new LinkedHashMap<>(requested);
        before.remove("dispensedItems"); before.remove("pharmacyStatus"); before.remove("dispensedMedicine");
        before.remove("dispensedQuantity"); before.remove("dispensedBy"); before.remove("dispensedAt");
        after.remove("dispensedItems"); after.remove("pharmacyStatus"); after.remove("dispensedMedicine");
        after.remove("dispensedQuantity"); after.remove("dispensedBy"); after.remove("dispensedAt");
        return before.equals(after);
    }

    private boolean changesClinicalContent(Map<String, Object> existing, Map<String, Object> requested) {
        var before = new LinkedHashMap<>(existing);
        var after = new LinkedHashMap<>(requested);
        before.remove("dispensedItems"); before.remove("pharmacyStatus"); before.remove("dispensedMedicine");
        before.remove("dispensedQuantity"); before.remove("dispensedBy"); before.remove("dispensedAt");
        after.remove("dispensedItems"); after.remove("pharmacyStatus"); after.remove("dispensedMedicine");
        after.remove("dispensedQuantity"); after.remove("dispensedBy"); after.remove("dispensedAt");
        return !before.equals(after);
    }

    @Override
    public void requireDeletable(String category, boolean doctor, Map<String, Object> existingFields) {
        if ("prescriptions".equals(category) && doctor
                && "Finalized".equalsIgnoreCase(String.valueOf(existingFields.get("clinicalStatus")))) {
            throw new AccessDeniedException("Finalized prescriptions are immutable; create a corrected prescription instead");
        }
    }
}
