package com.helios.backend.patient;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientProfileFlowTests {

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

    private String validProfileJson(String firstName) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "Doe",
                  "dateOfBirth": "1990-05-15",
                  "phoneNumber": "+1 555-123-4567",
                  "city": "Springfield",
                  "country": "USA",
                  "preferredLanguage": "en"
                }
                """.formatted(firstName);
    }

    @Test
    void getProfileBeforeCreationReturnsNotFound() throws Exception {
        String token = registerAndLogin("no-profile-yet@example.com", Role.PATIENT);

        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void createThenReadOwnProfile() throws Exception {
        String token = registerAndLogin("profile-owner@example.com", Role.PATIENT);

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validProfileJson("Alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"));

        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void updatingProfileOverwritesPreviousValues() throws Exception {
        String token = registerAndLogin("profile-updater@example.com", Role.PATIENT);

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validProfileJson("Original")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validProfileJson("Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void eachPatientOnlySeesTheirOwnProfile() throws Exception {
        String tokenA = registerAndLogin("patient-a@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("patient-b@example.com", Role.PATIENT);

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(validProfileJson("PatientA")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content(validProfileJson("PatientB")))
                .andExpect(status().isOk());

        // There is no endpoint that takes a profile/user ID from the
        // client at all — this confirms each token's own GET reflects
        // only that account's data, which is what prevents IDOR here.
        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.firstName").value("PatientA"));
        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.firstName").value("PatientB"));
    }

    @Test
    void providerRoleCannotAccessPatientProfileEndpoints() throws Exception {
        String token = registerAndLogin("a-provider@example.com", Role.PROVIDER);

        mockMvc.perform(get("/api/v1/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validProfileJson("Nope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/patient/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void futureDateOfBirthIsRejected() throws Exception {
        String token = registerAndLogin("future-dob@example.com", Role.PATIENT);

        String badJson = """
                {
                  "firstName": "Bad",
                  "lastName": "Date",
                  "dateOfBirth": "2999-01-01"
                }
                """;

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void blankFirstNameIsRejected() throws Exception {
        String token = registerAndLogin("blank-name@example.com", Role.PATIENT);

        String badJson = """
                {
                  "firstName": "",
                  "lastName": "Doe",
                  "dateOfBirth": "1990-01-01"
                }
                """;

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }
}
