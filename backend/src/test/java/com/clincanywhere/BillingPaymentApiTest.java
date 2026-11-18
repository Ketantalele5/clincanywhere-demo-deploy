package com.clincanywhere;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingPaymentApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cashReceiptUpdatesInvoiceAndRejectsOverpayment() throws Exception {
        String billingToken = token("billing");
        String invoiceId = createInvoice(billingToken, "clinic-a", "100.50");

        mockMvc.perform(post("/api/payments/clinic-a/invoices/{invoiceId}/cash", invoiceId)
                        .header("Authorization", "Bearer " + billingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", "20.25"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.amountPaise").value(2025))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/clinic-a/billing/{invoiceId}", invoiceId)
                        .header("Authorization", "Bearer " + billingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(20.25))
                .andExpect(jsonPath("$.status").value("Partially paid"));

        mockMvc.perform(post("/api/payments/clinic-a/invoices/{invoiceId}/cash", invoiceId)
                        .header("Authorization", "Bearer " + billingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", "80.26"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/payments/clinic-a/invoices/{invoiceId}", invoiceId)
                        .header("Authorization", "Bearer " + billingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void paymentEndpointsCannotAccessAnInvoiceInAnotherClinic() throws Exception {
        String billingToken = token("billing");
        String invoiceId = createInvoice(billingToken, "clinic-a", "75.00");

        mockMvc.perform(get("/api/payments/clinic-b/invoices/{invoiceId}", invoiceId)
                        .header("Authorization", "Bearer " + billingToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/payments/clinic-b/invoices/{invoiceId}/cash", invoiceId)
                        .header("Authorization", "Bearer " + billingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", "10.00"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/payments/clinic-a/invoices/{invoiceId}", invoiceId)
                        .header("Authorization", "Bearer " + billingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void doctorCannotRecordCashPayments() throws Exception {
        String billingToken = token("billing");
        String doctorToken = token("doctor");
        String invoiceId = createInvoice(billingToken, "clinic-a", "75.00");

        mockMvc.perform(post("/api/payments/clinic-a/invoices/{invoiceId}/cash", invoiceId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", "10.00"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments/clinic-a/invoices/{invoiceId}", invoiceId)
                        .header("Authorization", "Bearer " + billingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String createInvoice(String token, String clinicId, String amount) throws Exception {
        String response = mockMvc.perform(post("/api/{clinicId}/billing", clinicId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("id", "TEST-" + UUID.randomUUID(), "patient", "Payment test",
                                "date", "2026-10-24", "description", "Automated payment test", "amount", amount))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Objects.requireNonNull(objectMapper.readTree(response).get("id").textValue());
    }

    private String token(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "demo123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Objects.requireNonNull(objectMapper.readTree(response).get("accessToken").textValue());
    }

    private String json(Object value) throws Exception {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }
}
