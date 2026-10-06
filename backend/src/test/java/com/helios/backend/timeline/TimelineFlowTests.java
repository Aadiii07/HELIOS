package com.helios.backend.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helios.backend.identity.domain.Role;
import com.helios.backend.identity.dto.LoginRequest;
import com.helios.backend.identity.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimelineFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STRONG_PASSWORD = "Sup3rSecret!Pass";

    private String registerAndLogin(String email, Role role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, STRONG_PASSWORD, role))))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private void addManualObservation(String token, String name, String value, String date) throws Exception {
        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"displayName\":\"" + name + "\",\"value\":\"" + value + "\",\"effectiveDate\":\"" + date + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void timelineIncludesObservationEventsInChronologicalOrder() throws Exception {
        String token = registerAndLogin("timeline-chrono@example.com", Role.PATIENT);

        addManualObservation(token, "Hemoglobin A1c", "5.4", "2026-01-15");
        addManualObservation(token, "Hemoglobin A1c", "5.8", "2026-04-12");
        addManualObservation(token, "Hemoglobin A1c", "6.1", "2026-08-20");

        mockMvc.perform(get("/api/v1/timeline").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                // Most recent first.
                .andExpect(jsonPath("$.content[0].eventDate").value("2026-08-20"))
                .andExpect(jsonPath("$.content[1].eventDate").value("2026-04-12"))
                .andExpect(jsonPath("$.content[2].eventDate").value("2026-01-15"))
                .andExpect(jsonPath("$.content[0].eventType").value("OBSERVATION_RECORDED"));
    }

    @Test
    void timelineCanFilterByDateRange() throws Exception {
        String token = registerAndLogin("timeline-daterange@example.com", Role.PATIENT);

        addManualObservation(token, "Weight", "70", "2026-01-01");
        addManualObservation(token, "Weight", "71", "2026-06-01");

        mockMvc.perform(get("/api/v1/timeline?from=2026-05-01&to=2026-12-31").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].eventDate").value("2026-06-01"));
    }

    @Test
    void timelineCanFilterByObservationCode() throws Exception {
        String token = registerAndLogin("timeline-codefilter@example.com", Role.PATIENT);

        addManualObservation(token, "Weight", "70", "2026-01-01");
        addManualObservation(token, "Heart Rate", "72", "2026-01-01");

        mockMvc.perform(get("/api/v1/timeline?code=Weight").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("WEIGHT"));
    }

    @Test
    void timelineCanFilterBySource() throws Exception {
        String token = registerAndLogin("timeline-sourcefilter@example.com", Role.PATIENT);

        addManualObservation(token, "Weight", "70", "2026-01-01");

        mockMvc.perform(get("/api/v1/timeline?source=MANUAL_ENTRY").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/timeline?source=DOCUMENT_EXTRACTION").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void eachPatientOnlySeesTheirOwnTimeline() throws Exception {
        String tokenA = registerAndLogin("timeline-patient-a@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("timeline-patient-b@example.com", Role.PATIENT);

        addManualObservation(tokenA, "Weight", "70", "2026-01-01");

        mockMvc.perform(get("/api/v1/timeline").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void providerRoleCannotAccessTimeline() throws Exception {
        String token = registerAndLogin("timeline-provider@example.com", Role.PROVIDER);

        mockMvc.perform(get("/api/v1/timeline").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/timeline"))
                .andExpect(status().isUnauthorized());
    }
}
