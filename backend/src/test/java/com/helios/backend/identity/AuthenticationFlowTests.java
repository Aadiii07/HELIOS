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
    void malformedRequestBodyReturnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void changePasswordSucceedsAndOldPasswordNoLongerWorks() throws Exception {
        String email = "change-pw-success@example.com";
        String newPassword = "N3wSup3rSecret!";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"" + STRONG_PASSWORD + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent());

        // Old password must no longer work.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isUnauthorized());

        // New password must work.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        String email = "change-pw-wrongcurrent@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"WrongCurrent123!\",\"newPassword\":\"N3wSup3rSecret!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        // Original password must still work — the change must not have applied.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordRejectsWeakNewPassword() throws Exception {
        String email = "change-pw-weak@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, STRONG_PASSWORD, com.helios.backend.identity.domain.Role.PATIENT))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"" + STRONG_PASSWORD + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changePasswordRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"" + STRONG_PASSWORD + "\",\"newPassword\":\"N3wSup3rSecret!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
