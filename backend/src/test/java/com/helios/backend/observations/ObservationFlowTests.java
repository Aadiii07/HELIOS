package com.helios.backend.observations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helios.backend.identity.domain.Role;
import com.helios.backend.identity.dto.LoginRequest;
import com.helios.backend.identity.dto.RegisterRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ObservationFlowTests {

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

    private byte[] buildPdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 11);
                stream.newLineAtOffset(50, 700);
                for (String line : lines) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -20);
                }
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private String uploadPdfAndGetId(String token, List<String> lines) throws Exception {
        byte[] pdfBytes = buildPdf(lines);
        MockMultipartFile pdf = new MockMultipartFile("file", "lab-report.pdf", "application/pdf", pdfBytes);
        String body = mockMvc.perform(multipart("/api/v1/documents").file(pdf).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String firstCandidateId(String token, String docId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("id").asText();
    }

    @Test
    void creatingManualObservationSucceeds() throws Exception {
        String token = registerAndLogin("obs-manual@example.com", Role.PATIENT);

        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Body Weight\",\"value\":\"70.5\",\"unit\":\"kg\",\"effectiveDate\":\"2026-01-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BODY_WEIGHT"))
                .andExpect(jsonPath("$.displayName").value("Body Weight"))
                .andExpect(jsonPath("$.rawValue").value("70.5"))
                .andExpect(jsonPath("$.numericValue").value(70.5))
                .andExpect(jsonPath("$.source").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.confidence").value("HIGH"));
    }

    @Test
    void manualObservationWithFutureDateIsRejected() throws Exception {
        String token = registerAndLogin("obs-futuredate@example.com", Role.PATIENT);

        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Body Weight\",\"value\":\"70.5\",\"effectiveDate\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmingCandidateCreatesObservation() throws Exception {
        String token = registerAndLogin("obs-confirm@example.com", Role.PATIENT);
        String docId = uploadPdfAndGetId(token, List.of("Hemoglobin A1c 5.4 % 4.0-5.6"));
        String candidateId = firstCandidateId(token, docId);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CONFIRMED\",\"effectiveDate\":\"2026-01-15\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("HEMOGLOBIN_A1C"))
                .andExpect(jsonPath("$.content[0].rawValue").value("5.4"))
                .andExpect(jsonPath("$.content[0].source").value("DOCUMENT_EXTRACTION"))
                .andExpect(jsonPath("$.content[0].sourceDocumentId").value(docId))
                .andExpect(jsonPath("$.content[0].effectiveDate").value("2026-01-15"));
    }

    @Test
    void confirmingWithoutEffectiveDateDefaultsToUploadDate() throws Exception {
        String token = registerAndLogin("obs-defaultdate@example.com", Role.PATIENT);
        String docId = uploadPdfAndGetId(token, List.of("Glucose 98 mg/dL 70-99"));
        String candidateId = firstCandidateId(token, docId);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].effectiveDate").exists());
    }

    @Test
    void correctingCandidateCreatesObservationWithCorrectedValues() throws Exception {
        String token = registerAndLogin("obs-corrected@example.com", Role.PATIENT);
        String docId = uploadPdfAndGetId(token, List.of("Glucose 98 mg/dL 70-99"));
        String candidateId = firstCandidateId(token, docId);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CORRECTED\",\"correctedFieldLabel\":\"Fasting Glucose\",\"correctedRawValue\":\"100\",\"correctedUnit\":\"mg/dL\",\"effectiveDate\":\"2026-02-01\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("FASTING_GLUCOSE"))
                .andExpect(jsonPath("$.content[0].rawValue").value("100"));
    }

    @Test
    void rejectingCandidateCreatesNoObservation() throws Exception {
        String token = registerAndLogin("obs-rejected@example.com", Role.PATIENT);
        String docId = uploadPdfAndGetId(token, List.of("Glucose 98 mg/dL 70-99"));
        String candidateId = firstCandidateId(token, docId);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"REJECTED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void reviewingAnAlreadyReviewedCandidateIsRejected() throws Exception {
        String token = registerAndLogin("obs-double-review@example.com", Role.PATIENT);
        String docId = uploadPdfAndGetId(token, List.of("Glucose 98 mg/dL 70-99"));
        String candidateId = firstCandidateId(token, docId);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        // Reviewing again (even a different decision) must be rejected —
        // otherwise this would create a second, duplicate Observation.
        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANDIDATE_ALREADY_REVIEWED"));

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listCanFilterByCode() throws Exception {
        String token = registerAndLogin("obs-codefilter@example.com", Role.PATIENT);

        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Body Weight\",\"value\":\"70\",\"unit\":\"kg\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Heart Rate\",\"value\":\"72\",\"unit\":\"bpm\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/observations?code=body_weight").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Body Weight"));
    }

    @Test
    void eachPatientOnlySeesTheirOwnObservations() throws Exception {
        String tokenA = registerAndLogin("obs-patient-a@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("obs-patient-b@example.com", Role.PATIENT);

        mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Body Weight\",\"value\":\"70\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void cannotAccessAnotherPatientsObservationById() throws Exception {
        String tokenA = registerAndLogin("obs-owner@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("obs-intruder@example.com", Role.PATIENT);

        String body = mockMvc.perform(post("/api/v1/observations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Body Weight\",\"value\":\"70\",\"effectiveDate\":\"2026-01-01\"}"))
                .andReturn().getResponse().getContentAsString();
        String obsId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/v1/observations/" + obsId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OBSERVATION_NOT_FOUND"));
    }

    @Test
    void providerRoleCannotAccessObservationEndpoints() throws Exception {
        String token = registerAndLogin("obs-provider@example.com", Role.PROVIDER);

        mockMvc.perform(get("/api/v1/observations").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/observations"))
                .andExpect(status().isUnauthorized());
    }
}
