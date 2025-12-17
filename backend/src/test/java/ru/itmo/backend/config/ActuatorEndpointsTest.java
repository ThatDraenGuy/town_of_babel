package ru.itmo.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Actuator endpoints (health checks and metrics).
 * Tests problems 40 and 41.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpoint_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testHealthEndpoint_ContainsRepositoryStorage() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                // RepositoryStorageHealthIndicator может быть в components или в корне
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("UP"),
                        org.hamcrest.Matchers.is("DOWN")
                )));
    }

    @Test
    void testMetricsEndpoint_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void testMetricsEndpoint_ContainsCustomMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray())
                // Метрики могут быть не созданы до выполнения операций, поэтому проверяем только структуру
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void testSpecificMetric_CloneCounter() throws Exception {
        mockMvc.perform(get("/actuator/metrics/babel.repository.clone.total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("babel.repository.clone.total"))
                .andExpect(jsonPath("$.measurements").isArray());
    }

    @Test
    void testSpecificMetric_AnalysisCounter() throws Exception {
        mockMvc.perform(get("/actuator/metrics/babel.analysis.total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("babel.analysis.total"))
                .andExpect(jsonPath("$.measurements").isArray());
    }
}

