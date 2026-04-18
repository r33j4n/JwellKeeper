package com.jwellkeeper.billing.controller;

import com.jwellkeeper.billing.dto.BillResponse;
import com.jwellkeeper.billing.dto.CreateBillRequest;
import com.jwellkeeper.billing.dto.WhatsAppSendRequest;
import com.jwellkeeper.billing.dto.WhatsAppSendResponse;
import com.jwellkeeper.billing.service.BillingService;
import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bill")
public class BillingController {

    private final BillingService service;

    @PostMapping
    public ApiResponse<BillResponse> create(@Valid @RequestBody CreateBillRequest request) {
        return ApiResponse.success("Bill created", service.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<BillResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ApiResponse.success("Bills fetched", service.list(from, to, q, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BillResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Bill fetched", service.get(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] bytes = service.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bill-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/{id}/whatsapp")
    public ApiResponse<WhatsAppSendResponse> whatsapp(@PathVariable UUID id, @Valid @RequestBody WhatsAppSendRequest request) {
        return ApiResponse.success("WhatsApp send stub processed", service.sendWhatsApp(id, request.phoneNumber()));
    }
}
