package com.jwellkeeper.audit.controller;

import com.jwellkeeper.audit.dto.AuditCloseRequest;
import com.jwellkeeper.audit.dto.AuditAttemptCloseResponse;
import com.jwellkeeper.audit.dto.AuditForceCloseRequest;
import com.jwellkeeper.audit.dto.AuditReportResponse;
import com.jwellkeeper.audit.dto.AuditScanRequest;
import com.jwellkeeper.audit.dto.AuditScanResponse;
import com.jwellkeeper.audit.dto.StartAuditRequest;
import com.jwellkeeper.audit.dto.StockAuditResponse;
import com.jwellkeeper.audit.service.StockAuditService;
import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/audit", "/api/audits"})
public class StockAuditController {

    private final StockAuditService service;

    @PostMapping("/start")
    public ApiResponse<StockAuditResponse> start(@RequestBody(required = false) StartAuditRequest request) {
        return ApiResponse.success("Audit started", service.start(request == null ? new StartAuditRequest(null, null, null) : request));
    }

    @PostMapping("/scan")
    public ApiResponse<AuditScanResponse> scan(@Valid @RequestBody AuditScanRequest request) {
        return ApiResponse.success("Audit item scanned", service.scan(request));
    }

    @PostMapping("/{id}/attempt-close")
    public ApiResponse<AuditAttemptCloseResponse> attemptClose(@PathVariable UUID id) {
        return ApiResponse.success("Audit close attempted", service.attemptClose(id));
    }

    @PostMapping("/{id}/force-close")
    public ApiResponse<StockAuditResponse> forceClose(@PathVariable UUID id, @Valid @RequestBody AuditForceCloseRequest request) {
        return ApiResponse.success("Audit force closed", service.forceClose(id, request));
    }

    @PostMapping("/close")
    public ApiResponse<StockAuditResponse> close(@Valid @RequestBody AuditCloseRequest request) {
        return ApiResponse.success("Audit closed", service.close(request));
    }

    @GetMapping("/report")
    public ApiResponse<?> report(@RequestParam(required = false) UUID auditId, Pageable pageable) {
        if (auditId != null) {
            return ApiResponse.success("Audit report fetched", service.report(auditId));
        }
        return ApiResponse.success("Audit reports fetched", service.list(pageable));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] bytes = service.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
