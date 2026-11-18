package com.clincanywhere;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.jspecify.annotations.Nullable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MessagingApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private String writeJson(@Nullable Object value) throws Exception {
        return Objects.requireNonNull(json.writeValueAsString(Objects.requireNonNull(value)));
    }
    private static <T> T requireNonNull(@Nullable T value) {
        return Objects.requireNonNull(value);
    }
    private String token(String username) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", username, "password", "demo123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return requireNonNull(json.readTree(response).path("accessToken").textValue());
    }
    @Test void messagesPersistForConversationMembersAndRespectClinicMembership() throws Exception {
        String reception = token("reception");
        String doctor = token("doctor");
        mvc.perform(post("/api/clinic-a/messages").header("Authorization", "Bearer " + doctor)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("recipient", "reception", "body", "Please confirm follow-up"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sender").value("doctor"));
        mvc.perform(get("/api/clinic-a/messages?with=doctor").header("Authorization", "Bearer " + reception))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].body").value("Please confirm follow-up"));
        String admin = token("admin");
        String inviteResponse = mvc.perform(post("/api/clinic-a/patient-invitations").header("Authorization", "Bearer " + admin)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("patientRecordId", "PT-1001"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String patientResponse = mvc.perform(post("/api/auth/register").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "patient-limited", "password", "patient-pass-456",
                                "invitationToken", json.readTree(inviteResponse).get("invitationToken").asText()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String patient = json.readTree(patientResponse).get("accessToken").asText();
        mvc.perform(post("/api/clinic-b/messages").header("Authorization", "Bearer " + patient)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("recipient", "reception", "body", "wrong clinic"))))
                .andExpect(status().isNotFound());
    }
    @Test void whatsappRequiresConsentAndDisabledProviderFailsClearly() throws Exception {
        String doctor = token("doctor");
        mvc.perform(post("/api/clinic-a/whatsapp").header("Authorization", "Bearer " + doctor)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("phoneNumber", "+14155550100", "message", "Reminder", "consentConfirmed", false))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/clinic-a/whatsapp").header("Authorization", "Bearer " + doctor)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("phoneNumber", "+14155550100", "message", "Reminder", "consentConfirmed", true))))
                .andExpect(status().isBadRequest());
    }

    @Test void invitedPatientCanMessageClinicDoctorButNotOtherStaff() throws Exception {
        String admin = token("admin");
        String inviteResponse = mvc.perform(post("/api/clinic-a/patient-invitations").header("Authorization", "Bearer " + admin)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("patientRecordId", "PT-1002"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String invite = json.readTree(inviteResponse).get("invitationToken").asText();
        String registerResponse = mvc.perform(post("/api/auth/register").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "patient-priya", "password", "patient-pass-123", "invitationToken", invite))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String patient = json.readTree(registerResponse).get("accessToken").asText();
        token("doctor");
        mvc.perform(post("/api/clinic-a/messages").header("Authorization", "Bearer " + patient)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("recipient", "doctor", "body", "I have a question about my follow-up"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/clinic-a/messages").header("Authorization", "Bearer " + patient)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("recipient", "reception", "body", "Please help"))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/clinic-a/messages/contacts").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.role == 'DOCTOR')]").isNotEmpty());
    }

    @Test void staffMustSelectPatientWhenCreatingSupportTicket() throws Exception {
        String reception = token("reception");
        mvc.perform(post("/api/clinic-a/support").header("Authorization", "Bearer " + reception)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("subject", "Account help", "message", "Please check my appointment"))))
                .andExpect(status().isBadRequest());
    }

    @Test void patientCannotWriteClinicProfessionalRegistryProfile() throws Exception {
        String admin = token("admin");
        String inviteResponse = mvc.perform(post("/api/clinic-a/patient-invitations").header("Authorization", "Bearer " + admin)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("patientRecordId", "PT-1001"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String invitation = json.readTree(inviteResponse).get("invitationToken").asText();
        String patientResponse = mvc.perform(post("/api/auth/register").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "patient-registry-check", "password", "patient-pass-123", "invitationToken", invitation))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String patient = json.readTree(patientResponse).get("accessToken").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/clinic-a/digital-health/professionals/me")
                        .header("Authorization", "Bearer " + patient).contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("hprId", "reference", "status", "VERIFIED"))))
                .andExpect(status().isForbidden());
    }
}
