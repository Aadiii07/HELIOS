package com.helios.backend.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helios.backend.identity.domain.Role;
import com.helios.backend.identity.dto.LoginRequest;
import com.helios.backend.identity.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STRONG_PASSWORD = "Sup3rSecret!Pass";

    // Minimal but genuinely valid PDF signature + trailing bytes.
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 fake but signature-valid test content".getBytes();

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

    private MockMultipartFile validPdfPart() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", VALID_PDF_BYTES);
    }

    private String uploadAndGetId(String token) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/documents")
                        .file(validPdfPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void uploadValidPdfSucceeds() throws Exception {
        String token = registerAndLogin("doc-upload@example.com", Role.PATIENT);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(validPdfPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("report.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.checksumSha256").isNotEmpty());
    }

    @Test
    void uploadRejectsContentTypeSpoofing() throws Exception {
        String token = registerAndLogin("doc-spoof@example.com", Role.PATIENT);

        // Claims to be a PDF, but the bytes don't start with %PDF.
        MockMultipartFile spoofed = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not actually a pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(spoofed)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
    }

    @Test
    void uploadRejectsUnsupportedContentType() throws Exception {
        String token = registerAndLogin("doc-unsupported@example.com", Role.PATIENT);

        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
    }

    @Test
    void uploadRejectsOversizedFile() throws Exception {
        String token = registerAndLogin("doc-oversized@example.com", Role.PATIENT);

        byte[] oversized = new byte[21 * 1024 * 1024]; // over the 20MB default limit
        Arrays.fill(oversized, (byte) 'x');
        MockMultipartFile bigFile = new MockMultipartFile("file", "huge.pdf", "application/pdf", oversized);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(bigFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOnlyReturnsOwnDocuments() throws Exception {
        String tokenA = registerAndLogin("doc-patient-a@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("doc-patient-b@example.com", Role.PATIENT);

        uploadAndGetId(tokenA);
        uploadAndGetId(tokenB);
        uploadAndGetId(tokenB);

        mockMvc.perform(get("/api/v1/documents").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/documents").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void cannotAccessAnotherPatientsDocumentMetadata() throws Exception {
        String tokenA = registerAndLogin("doc-owner@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("doc-intruder@example.com", Role.PATIENT);

        String docId = uploadAndGetId(tokenA);

        mockMvc.perform(get("/api/v1/documents/" + docId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void cannotDownloadAnotherPatientsDocumentContent() throws Exception {
        String tokenA = registerAndLogin("doc-owner2@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("doc-intruder2@example.com", Role.PATIENT);

        String docId = uploadAndGetId(tokenA);

        mockMvc.perform(get("/api/v1/documents/" + docId + "/content").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanDownloadOwnDocumentContent() throws Exception {
        String token = registerAndLogin("doc-download@example.com", Role.PATIENT);
        String docId = uploadAndGetId(token);

        mockMvc.perform(get("/api/v1/documents/" + docId + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PDF_BYTES));
    }

    @Test
    void deletingRemovesDocumentFromSubsequentAccess() throws Exception {
        String token = registerAndLogin("doc-delete@example.com", Role.PATIENT);
        String docId = uploadAndGetId(token);

        mockMvc.perform(delete("/api/v1/documents/" + docId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/" + docId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteAnotherPatientsDocument() throws Exception {
        String tokenA = registerAndLogin("doc-owner3@example.com", Role.PATIENT);
        String tokenB = registerAndLogin("doc-intruder3@example.com", Role.PATIENT);

        String docId = uploadAndGetId(tokenA);

        mockMvc.perform(delete("/api/v1/documents/" + docId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Confirm it's still there for the actual owner.
        mockMvc.perform(get("/api/v1/documents/" + docId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void providerRoleCannotAccessDocumentEndpoints() throws Exception {
        String token = registerAndLogin("doc-provider@example.com", Role.PROVIDER);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(validPdfPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/documents").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());
    }
}
