package com.helios.backend.documents.controller;

import com.helios.backend.common.web.ClientIpResolver;
import com.helios.backend.documents.dto.DocumentResponse;
import com.helios.backend.documents.service.DocumentService;
import com.helios.backend.identity.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("hasRole('PATIENT')")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest servletRequest
    ) {
        return service.upload(user.id(), file, ClientIpResolver.resolve(servletRequest));
    }

    @GetMapping
    public Page<DocumentResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return service.list(user.id(), pageable);
    }

    @GetMapping("/{id}")
    public DocumentResponse getMetadata(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return service.getMetadata(user.id(), id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> downloadContent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            HttpServletRequest servletRequest
    ) {
        var doc = service.downloadContent(user.id(), id, ClientIpResolver.resolve(servletRequest));

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(doc.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(doc.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            HttpServletRequest servletRequest
    ) {
        service.delete(user.id(), id, ClientIpResolver.resolve(servletRequest));
    }
}
