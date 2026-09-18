package com.helios.backend.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * End-to-end auth flow against the real application context and a
 * real PostgreSQL instance (per backend/.env). @Transactional rolls
 * back all DB changes after each test so re-runs don't collide on the
 * unique email constraint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STRONG_PASSWORD = "Sup3rSecret!Pass";

    @Test
    void registerThenLoginThenAccessProtectedEndpoint() throws Exception {
        String email = "patient.flow@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PATIENT"));

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginBody).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = "duplicate@example.com";
        RegisterRequest request = new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("weak@example.com", "short", com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerRejectsPrivilegedRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("wannabe-admin@example.com", STRONG_PASSWORD,
                                        com.helios.backend.identity.domain.Role.SYSTEM_ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = "wrongpass@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void protectedEndpointRejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
