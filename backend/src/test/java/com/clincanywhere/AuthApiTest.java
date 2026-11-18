package com.clincanywhere;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cors.allowed-origins=https://clincyanywhere-preview.pages.dev")
class AuthApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String writeJson(@Nullable Object value) throws Exception {
        return Objects.requireNonNull(json.writeValueAsString(Objects.requireNonNull(value)));
    }

    private static <T> T requireNonNull(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Test void protectedRoutePreflightIsHandledBeforeAuthentication() throws Exception {
        mvc.perform(options("/api/clinics")
                        .header("Origin", "https://clincyanywhere-preview.pages.dev")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://clincyanywhere-preview.pages.dev"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("authorization")));
    }

    @Test void loginIssuesJwtAndProtectedRoutesRequireIt() throws Exception {
        mvc.perform(get("/api/clinic-a/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentTypeCompatibleWith(requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.traceId").exists());
        String response = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "admin", "password", "demo123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/clinic-a/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        var outsideDoctor = Objects.requireNonNull(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("outside").roles("DOCTOR"));
        mvc.perform(get("/api/clinic-a/patients").header("Authorization", "Bearer " + token)
                        .with(outsideDoctor))
                .andExpect(status().isNotFound());
    }

    @Test void invalidCredentialsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "admin", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test void staffCannotBeProvisionedForUnknownClinic() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "admin", "password", "demo123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("accessToken").asText();

        mvc.perform(post("/api/clinics/unknown-clinic/team").header("Authorization", "Bearer " + token)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of(
                                "username", "orphanstaff", "password", "secure-pass-123", "displayName", "Orphan Staff", "role", "DOCTOR"))))
                .andExpect(status().isNotFound());
    }

    @Test void allClinicStaffCanResolveTheirClinicWithoutSettingsReadPermission() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "billing", "password", "demo123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("accessToken").asText();

        mvc.perform(get("/api/clinics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("clinic-a"));
    }

    @Test void invitedPatientGetsOnlyLinkedRecordAndClinicAccess() throws Exception {
        String adminResponse = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "admin", "password", "demo123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adminToken = json.readTree(adminResponse).get("accessToken").asText();
        String invitationResponse = mvc.perform(post("/api/clinic-a/patient-invitations").header("Authorization", "Bearer " + adminToken)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("patientRecordId", "PT-1001"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String invitation = json.readTree(invitationResponse).get("invitationToken").asText();
        String response = mvc.perform(post("/api/auth/register").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "newpatient", "password", "secure-pass-123", "invitationToken", invitation))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/clinics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/clinic-a/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("PT-1001"));
        mvc.perform(get("/api/clinic-a/records").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clinic-a/patients/PT-1002").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clinic-a/patients").header("Authorization", "Bearer " + token)
                        .contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("name", "Unauthorized patient"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clinic-b/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test void alteredJwtCannotAuthenticate() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(requireNonNull(MediaType.APPLICATION_JSON))
                        .content(writeJson(Map.of("username", "admin", "password", "demo123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("accessToken").asText();
        String altered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        mvc.perform(get("/api/clinic-a/patients").header("Authorization", "Bearer " + altered))
                .andExpect(status().isUnauthorized());
    }
}
