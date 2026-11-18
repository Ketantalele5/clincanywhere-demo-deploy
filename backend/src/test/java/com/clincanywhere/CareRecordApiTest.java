package com.clincanywhere;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
@SuppressWarnings("null")
class CareRecordApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.clincanywhere.service.CareRecordService careRecords;

    @Test
    void clinicDataCanBeCreatedUpdatedAndDeleted() throws Exception {
        mockMvc.perform(get("/api/clinic-a/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        String created = mockMvc.perform(post("/api/clinic-a/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("name", "Demo Patient", "age", 31, "status", "Active"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(put("/api/clinic-a/patients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("name", "Updated Patient", "status", "Follow-up"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Patient"));

        mockMvc.perform(get("/api/clinic-a/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Patient"));

        mockMvc.perform(get("/api/clinic-a/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].name").value("Updated Patient"));

        mockMvc.perform(delete("/api/clinic-a/patients/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clinic-a/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mockMvc.perform(get("/api/clinic-b/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mockMvc.perform(get("/api/clinic-a/patients/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void demoDataCanBeRestoredForOneClinic() throws Exception {
        mockMvc.perform(delete("/api/clinic-a/demo-data"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clinic-a/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mockMvc.perform(get("/api/clinic-b/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void nonAdminCannotResetClinicDemoData() throws Exception {
        mockMvc.perform(delete("/api/clinic-a/demo-data").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotReadSettingsButCanStillResolveClinicProfile() throws Exception {
        var billing = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("billing").roles("BILLING");
        mockMvc.perform(get("/api/clinic-a/settings").with(billing)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clinics").with(billing)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void clinicMembershipIsEnforcedAtTheServiceBoundary() {
        var nonMember = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                "outsider", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(nonMember);
        try {
            org.junit.jupiter.api.Assertions.assertThrows(com.clincanywhere.common.exception.NotFoundException.class,
                    () -> careRecords.list("clinic-b", "patients", nonMember));
            org.junit.jupiter.api.Assertions.assertThrows(com.clincanywhere.common.exception.NotFoundException.class,
                    () -> careRecords.create("clinic-b", "patients", new com.clincanywhere.dto.RecordRequest(), "outsider", false));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
