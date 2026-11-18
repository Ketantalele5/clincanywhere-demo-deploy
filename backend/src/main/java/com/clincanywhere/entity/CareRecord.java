package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "care_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "category", "external_id"})
})
public class CareRecord extends BaseEntity {
    @Column(name = "clinic_id", nullable = false, length = 40)
    private String clinicId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "external_id", nullable = false, length = 80)
    private String externalId;

    @Column(name = "payload_json", nullable = false, length = 30000)
    private String payloadJson;

    @Column(name = "search_name", nullable = false, length = 64)
    private String searchName = "";
    @Column(name = "search_phone", nullable = false, length = 64)
    private String searchPhone = "";
    @Column(name = "search_id", nullable = false, length = 64)
    private String searchId = "";

    @jakarta.persistence.Version
    @Column(name = "record_version", nullable = false)
    private long version;

    protected CareRecord() {
    }

    public CareRecord(String clinicId, String category, String externalId, String payloadJson) {
        this.clinicId = clinicId;
        this.category = category;
        this.externalId = externalId;
        this.payloadJson = payloadJson;
    }

    public String getClinicId() {
        return clinicId;
    }

    public String getCategory() {
        return category;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getSearchName() { return searchName; }
    public String getSearchPhone() { return searchPhone; }
    public String getSearchId() { return searchId; }
    public void replaceSearchIndexes(String searchName, String searchPhone, String searchId) {
        this.searchName = searchName == null ? "" : searchName;
        this.searchPhone = searchPhone == null ? "" : searchPhone;
        this.searchId = searchId == null ? "" : searchId;
    }

    public void replacePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) throw new IllegalArgumentException("Payload is required");
        this.payloadJson = payloadJson;
    }
}
