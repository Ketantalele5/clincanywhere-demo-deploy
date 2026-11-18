package com.clincanywhere.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.clincanywhere.entity.CareRecord;
import com.clincanywhere.repository.CareRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"dev", "test", "demo & !prod"})
public class DemoData implements CommandLineRunner {
    private final CareRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final com.clincanywhere.service.FieldEncryptionService encryption;

    public DemoData(CareRecordRepository repository, ObjectMapper objectMapper,
                    com.clincanywhere.service.FieldEncryptionService encryption) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.encryption = encryption;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        for (String clinic : new String[] {"clinic-a", "clinic-b", "clinic-c"}) {
            seedClinic(clinic);
        }
    }

    @Transactional
    public void resetClinic(String clinic) {
        repository.deleteByClinicId(clinic);
        repository.flush();
        seedClinic(clinic);
    }

    private void seedClinic(String clinic) {
        LocalDate todayDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String today = todayDate.toString();
        save(clinic, "patients", "PT-1001", map("name", "Aarav Sharma", "age", 34, "gender", "Male",
                "phone", "9876543210", "email", "aarav@example.com", "blood", "B+",
                "doctor", "Dr. Meera Joshi", "condition", "Hypertension", "status", "Active", "joined", today));
        save(clinic, "patients", "PT-1002", map("name", "Priya Patel", "age", 28, "gender", "Female",
                "phone", "9822012345", "email", "priya@example.com", "blood", "O+",
                "doctor", "Dr. Rohan Kulkarni", "condition", "Migraine", "status", "Active", "joined", today));
        save(clinic, "patients", "PT-1003", map("name", "Rohan Deshmukh", "age", 52, "gender", "Male",
                "phone", "9765432109", "email", "rohan@example.com", "blood", "A+",
                "doctor", "Dr. Meera Joshi", "condition", "Diabetes Type 2", "status", "Follow-up", "joined", today));
        save(clinic, "patients", "PT-1004", map("name", "Ananya Shah", "age", 41, "gender", "Female",
                "phone", "9890011223", "email", "ananya@example.com", "blood", "AB+",
                "doctor", "Dr. Neha Rao", "condition", "Post-op review", "status", "Active", "joined", today));
        save(clinic, "patients", "PT-1005", map("name", "Vikram Singh", "age", 63, "gender", "Male",
                "phone", "9823012233", "email", "vikram@example.com", "blood", "O-",
                "doctor", "Dr. Rohan Kulkarni", "condition", "Cardiac review", "status", "Follow-up", "joined", today));
        save(clinic, "patients", "PT-1006", map("name", "Sneha Kulkarni", "age", 23, "gender", "Female",
                "phone", "9766112233", "email", "sneha@example.com", "blood", "A-",
                "doctor", "Dr. Neha Rao", "condition", "Fever", "status", "Active", "joined", today));

        save(clinic, "doctors", "DR-001", map("name", "Dr. Meera Joshi", "specialty", "General Medicine",
                "phone", "9822001100", "email", "meera@example.com", "shift", "09:00 – 17:00", "status", "Available"));
        save(clinic, "doctors", "DR-002", map("name", "Dr. Rohan Kulkarni", "specialty", "Neurology",
                "phone", "9822002200", "email", "rohan.k@example.com", "shift", "10:00 – 18:00", "status", "Available"));
        save(clinic, "doctors", "DR-003", map("name", "Dr. Neha Rao", "specialty", "Cardiology",
                "phone", "9822003300", "email", "neha@example.com", "shift", "08:00 – 16:00", "status", "In surgery"));

        save(clinic, "appointments", "AP-201", map("patient", "Aarav Sharma", "doctor", "Dr. Meera Joshi",
                "department", "General Medicine", "date", today, "time", "09:30", "type", "Consultation", "status", "Confirmed"));
        save(clinic, "appointments", "AP-202", map("patient", "Priya Patel", "doctor", "Dr. Rohan Kulkarni",
                "department", "Neurology", "date", today, "time", "10:00", "type", "Follow-up", "status", "Checked in"));
        save(clinic, "appointments", "AP-203", map("patient", "Rohan Deshmukh", "doctor", "Dr. Meera Joshi",
                "department", "General Medicine", "date", today, "time", "11:30", "type", "Review", "status", "Waiting"));
        save(clinic, "appointments", "AP-204", map("patient", "Ananya Shah", "doctor", "Dr. Neha Rao",
                "department", "Cardiology", "date", today, "time", "14:00", "type", "Consultation", "status", "Confirmed"));

        save(clinic, "records", "MR-301", map("patient", "Rohan Deshmukh", "doctor", "Dr. Meera Joshi",
                "date", today, "diagnosis", "Type 2 diabetes", "notes", "Routine review; continue monitoring.",
                "prescription", "Metformin as prescribed", "status", "Reviewed"));
        save(clinic, "records", "MR-302", map("patient", "Priya Patel", "doctor", "Dr. Rohan Kulkarni",
                "date", today, "diagnosis", "Migraine", "notes", "Follow-up after two weeks.",
                "prescription", "See prescription", "status", "Follow-up"));

        save(clinic, "prescriptions", "RX-401", map("patient", "Rohan Deshmukh", "doctor", "Dr. Meera Joshi",
                "date", today, "medicines", "Metformin 500 mg — as directed", "instructions", "Take with food.",
                "followupDate", today, "clinicalStatus", "Finalized", "pharmacyStatus", "Processing",
                "whatsappStatus", "Not sent", "notes", "Sample prescription"));
        save(clinic, "prescriptions", "RX-402", map("patient", "Priya Patel", "doctor", "Dr. Rohan Kulkarni",
                "date", today, "medicines", "As per clinician's plan", "instructions", "Contact the clinic if symptoms persist.",
                "followupDate", today, "clinicalStatus", "Draft", "pharmacyStatus", "Draft",
                "whatsappStatus", "Not sent", "notes", ""));
        save(clinic, "followups", "FU-501", map("patient", "Vikram Singh", "doctor", "Dr. Rohan Kulkarni",
                "dueDate", today, "reminderDays", 2, "channel", "WhatsApp preview",
                "status", "Scheduled", "appointmentStatus", "Pending"));
        save(clinic, "laboratory", "LB-601", map("patient", "Aarav Sharma", "test", "Complete blood count",
                "orderedBy", "Dr. Meera Joshi", "date", today, "priority", "Routine", "status", "Sample collected"));

        save(clinic, "pharmacy", "MED-701", map("name", "Paracetamol 500 mg", "category", "Analgesic",
                "stock", 125, "reorder", 40, "price", 4.5, "expiry", todayDate.plusYears(2).toString()));
        save(clinic, "pharmacy", "MED-702", map("name", "Amoxicillin 250 mg", "category", "Antibiotic",
                "stock", 34, "reorder", 45, "price", 8.0, "expiry", todayDate.plusMonths(9).toString()));
        save(clinic, "beds", "BED-01", map("ward", "General", "type", "Standard", "patient", "—",
                "status", "Available", "dailyRate", 1800));
        save(clinic, "beds", "BED-02", map("ward", "Cardiac", "type", "Private", "patient", "Vikram Singh",
                "status", "Occupied", "dailyRate", 4200));
        save(clinic, "billing", "INV-801", map("patient", "Aarav Sharma", "date", today,
                "description", "Consultation and diagnostics", "amount", 2800, "paid", 1800, "status", "Partially paid"));
        save(clinic, "billing", "INV-802", map("patient", "Priya Patel", "date", today,
                "description", "Neurology consultation", "amount", 1500, "paid", 1500, "status", "Paid"));
        save(clinic, "inventory", "INV-901", map("name", "Examination gloves", "category", "Consumables",
                "stock", 24, "reorder", 50, "unit", "boxes", "supplier", "MedSupply Co."));
        save(clinic, "inventory", "INV-902", map("name", "Syringes 5 ml", "category", "Consumables",
                "stock", 180, "reorder", 100, "unit", "pieces", "supplier", "CareSource Ltd."));

        save(clinic, "instantreminders", "IR-1001", map("patientName", "Aarav Sharma", "phone", "9876543210",
                "channel", "WhatsApp preview", "message", "Hello, this is a reminder from your clinic about your upcoming appointment.",
                "status", "Ready for preview", "createdAt", today));
        save(clinic, "settings", "clinic-profile", map("name", clinicName(clinic), "phone", "+91 20 0000 0000", "timezone", "Asia/Kolkata"));
    }

    private void save(String clinic, String category, String id, Map<String, Object> values) {
        values.put("id", id);
        try {
            CareRecord record = new CareRecord(clinic, category, id, objectMapper.writeValueAsString(values));
            record.replaceSearchIndexes(searchHash(values, "name", "patient", "patientName", "email"),
                    searchHash(values, "phone"), encryption.fingerprint(id.toLowerCase(java.util.Locale.ROOT)));
            repository.save(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not prepare demo data", exception);
        }
    }

    private String searchHash(Map<String, Object> values, String... fields) {
        for (String field : fields) {
            Object value = values.get(field);
            if (value != null && !value.toString().isBlank()) return encryption.fingerprint(value.toString().trim().toLowerCase(java.util.Locale.ROOT));
        }
        return "";
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private String clinicName(String clinic) {
        return switch (clinic) {
            case "clinic-b" -> "Sunrise Family Clinic";
            case "clinic-c" -> "Lotus Multispeciality";
            default -> "ClincyAnywhere Health Centre";
        };
    }
}
