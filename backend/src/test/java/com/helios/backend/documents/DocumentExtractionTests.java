package com.helios.backend.documents;

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
class DocumentExtractionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STRONG_PASSWORD = "Sup3rSecret!Pass";

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, STRONG_PASSWORD, Role.PATIENT))))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, STRONG_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("accessToken").asText();
    }

    /** Generates a real, valid PDF with the given lines of text — not fake bytes. */
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

    @Test
    void uploadingPdfExtractsLabValueCandidates() throws Exception {
        String token = registerAndLogin("extract-basic@example.com");
        String docId = uploadPdfAndGetId(token, List.of(
                "Hemoglobin A1c    5.4    %    4.0-5.6",
                "Glucose, Fasting    98    mg/dl    70-99"
        ));

        mockMvc.perform(get("/api/v1/documents/" + docId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractionStatus").value("PROCESSED"));

        mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fieldLabel").value("Hemoglobin A1c"))
                .andExpect(jsonPath("$[0].rawValue").value("5.4"))
                .andExpect(jsonPath("$[0].reviewStatus").value("PENDING"));
    }

    @Test
    void uploadingImageMarksUnsupportedFormatWithNoCandidates() throws Exception {
        String token = registerAndLogin("extract-image@example.com");

        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile png = new MockMultipartFile("file", "scan.png", "image/png", pngBytes);

        String body = mockMvc.perform(multipart("/api/v1/documents").file(png).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extractionStatus").value("UNSUPPORTED_FORMAT"))
                .andReturn().getResponse().getContentAsString();
        String docId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void confirmingCandidateUpdatesReviewStatus() throws Exception {
        String token = registerAndLogin("extract-confirm@example.com");
        String docId = uploadPdfAndGetId(token, List.of("LDL Cholesterol    130    mg/dl    <100"));

        String candidatesBody = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String candidateId = objectMapper.readTree(candidatesBody).get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("CONFIRMED"));
    }

    @Test
    void correctingCandidatePreservesOriginalAlongsideCorrection() throws Exception {
        String token = registerAndLogin("extract-correct@example.com");
        String docId = uploadPdfAndGetId(token, List.of("Glucose    99    mg/dl    70-99"));

        String candidatesBody = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String candidateId = objectMapper.readTree(candidatesBody).get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CORRECTED\",\"correctedFieldLabel\":\"Glucose (Fasting)\",\"correctedRawValue\":\"100\",\"correctedUnit\":\"mg/dL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("CORRECTED"))
                .andExpect(jsonPath("$.correctedRawValue").value("100"))
                // Original extracted value must still be there, untouched.
                .andExpect(jsonPath("$.rawValue").value("99"));
    }

    @Test
    void correctingWithoutRequiredFieldsIsRejected() throws Exception {
        String token = registerAndLogin("extract-correct-missing@example.com");
        String docId = uploadPdfAndGetId(token, List.of("Glucose    99    mg/dl    70-99"));

        String candidatesBody = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String candidateId = objectMapper.readTree(candidatesBody).get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"CORRECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reviewStatusPendingIsRejected() throws Exception {
        String token = registerAndLogin("extract-pending@example.com");
        String docId = uploadPdfAndGetId(token, List.of("Glucose    99    mg/dl    70-99"));

        String candidatesBody = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String candidateId = objectMapper.readTree(candidatesBody).get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/documents/" + docId + "/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reviewStatus\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractsFromSingleSpaceSeparatedRealWorldLayout() throws Exception {
        // Verbatim shape of a real user-uploaded PDF that the previous
        // (2+-space-column) parser missed entirely: single spaces
        // between columns, multi-word labels, and a header/footer
        // that must NOT be mistaken for data rows.
        String token = registerAndLogin("extract-realworld@example.com");
        String docId = uploadPdfAndGetId(token, List.of(
                "HELIOS Synthetic Clean Laboratory Report",
                "Patient: Test Patient Alpha | Patient ID: SYN-001 | Collection Date: 2026-01-15 | Report Date: 2026-01-16",
                "Laboratory Results",
                "Test Result Unit Reference Range",
                "Hemoglobin A1c 5.4 % 4.0-5.6",
                "Fasting Glucose 92 mg/dL 70-100",
                "Hemoglobin 14.2 g/dL 12.0-16.0",
                "Creatinine 0.9 mg/dL 0.6-1.2",
                "LDL Cholesterol 118 mg/dL 0-100",
                "HDL Cholesterol 52 mg/dL >40",
                "Triglycerides 145 mg/dL 0-150",
                "Total Cholesterol 199 mg/dL 0-200",
                "Synthetic document for software testing only. No real patient information."
        ));

        String body = mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Exactly the 8 real data rows — none of the header/
                // patient-info/footer lines should be mistaken for one.
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].fieldLabel").value("Hemoglobin A1c"))
                .andExpect(jsonPath("$[0].rawValue").value("5.4"))
                .andExpect(jsonPath("$[0].unit").value("%"))
                .andExpect(jsonPath("$[0].referenceRange").value("4.0-5.6"))
                .andReturn().getResponse().getContentAsString();

        List<String> labels = new java.util.ArrayList<>();
        for (var node : objectMapper.readTree(body)) {
            labels.add(node.get("fieldLabel").asText());
        }
        org.assertj.core.api.Assertions.assertThat(labels).containsExactlyInAnyOrder(
                "Hemoglobin A1c", "Fasting Glucose", "Hemoglobin", "Creatinine",
                "LDL Cholesterol", "HDL Cholesterol", "Triglycerides", "Total Cholesterol"
        );
    }

    @Test
    void cannotListAnotherPatientsCandidates() throws Exception {
        String tokenA = registerAndLogin("extract-owner@example.com");
        String tokenB = registerAndLogin("extract-intruder@example.com");

        String docId = uploadPdfAndGetId(tokenA, List.of("Glucose    99    mg/dl    70-99"));

        mockMvc.perform(get("/api/v1/documents/" + docId + "/candidates").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }
}
