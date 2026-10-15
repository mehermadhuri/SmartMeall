package com.smartmeal.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiServiceConfigTest {
    @Test
    void addsHttpsSchemeToHostOnlyServiceReference() {
        assertEquals("https://smartmeal-ai.onrender.com",
                AiServiceConfig.normalizeBaseUrl("smartmeal-ai.onrender.com"));
    }

    @Test
    void preservesConfiguredScheme() {
        assertEquals("http://ai-service:5001",
                AiServiceConfig.normalizeBaseUrl("http://ai-service:5001"));
    }
}
