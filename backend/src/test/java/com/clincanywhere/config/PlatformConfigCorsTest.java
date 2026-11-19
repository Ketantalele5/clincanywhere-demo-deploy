package com.clincanywhere.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class PlatformConfigCorsTest {
    @Test
    void allowsConfiguredOriginsAndLocalDevelopmentHostsOnly() {
        var source = new PlatformConfig().corsConfigurationSource("https://clinic.example, https://preview.example");
        var request = new MockHttpServletRequest("GET", "/api/health");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertEquals("https://clinic.example", cors.checkOrigin("https://clinic.example"));
        assertEquals("https://preview.example", cors.checkOrigin("https://preview.example"));
        assertEquals("http://127.0.0.1:5173", cors.checkOrigin("http://127.0.0.1:5173"));
        assertNull(cors.checkOrigin("https://untrusted.pages.dev"));
        assertNull(cors.checkOrigin("https://untrusted.eu.org"));
        assertEquals(java.util.List.of("Authorization"), cors.checkHeaders(java.util.List.of("Authorization")));
        assertEquals(3600L, cors.getMaxAge());
    }
}
