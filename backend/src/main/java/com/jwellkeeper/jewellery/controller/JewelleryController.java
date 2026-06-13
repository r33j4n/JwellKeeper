package com.jwellkeeper.jewellery.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.jewellery.dto.ArchivedJewelleryRequest;
import com.jwellkeeper.jewellery.dto.CreateJewelleryRequest;
import com.jwellkeeper.jewellery.dto.CreateJewelleryTypeRequest;
import com.jwellkeeper.jewellery.dto.JewelleryResponse;
import com.jwellkeeper.jewellery.dto.JewelleryTypeResponse;
import com.jwellkeeper.jewellery.dto.MarkFoundRequest;
import com.jwellkeeper.jewellery.dto.QrImageResponse;
import com.jwellkeeper.jewellery.dto.QrResolveRequest;
import com.jwellkeeper.jewellery.dto.UpdateJewelleryRequest;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.service.JewelleryService;
import com.jwellkeeper.jewellery.service.JewelleryTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jewellery")
public class JewelleryController {

    private final JewelleryService jewelleryService;
    private final JewelleryTypeService typeService;

    @PostMapping("/types")
    public ApiResponse<JewelleryTypeResponse> createType(@Valid @RequestBody CreateJewelleryTypeRequest request) {
        return ApiResponse.success("Jewellery type created", typeService.create(request));
    }

    @GetMapping("/types")
    public ApiResponse<List<JewelleryTypeResponse>> listTypes() {
        return ApiResponse.success("Jewellery types fetched", typeService.list());
    }

    @PostMapping
    public ApiResponse<JewelleryResponse> create(@Valid @RequestBody CreateJewelleryRequest request) {
        return ApiResponse.success("Jewellery created successfully", jewelleryService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<JewelleryResponse>> list(
            @RequestParam(required = false) JewelleryStatus status,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) String karat,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) java.math.BigDecimal minWeight,
            @RequestParam(required = false) java.math.BigDecimal maxWeight,
            Pageable pageable
    ) {
        return ApiResponse.success("Jewellery fetched", jewelleryService.list(status, typeId, karat, q, minWeight, maxWeight, pageable));
    }

    @PostMapping("/archived/search")
    public ApiResponse<PageResponse<JewelleryResponse>> archived(@RequestBody ArchivedJewelleryRequest request, Pageable pageable) {
        return ApiResponse.success("Archived jewellery fetched", jewelleryService.archived(request, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<JewelleryResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Jewellery fetched", jewelleryService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<JewelleryResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateJewelleryRequest request) {
        return ApiResponse.success("Jewellery updated", jewelleryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        jewelleryService.delete(id);
        return ApiResponse.success("Jewellery deleted");
    }

    @GetMapping("/{id}/qr")
    public ApiResponse<QrImageResponse> getQr(@PathVariable UUID id) {
        return ApiResponse.success("QR generated", jewelleryService.getQr(id));
    }

    @PostMapping("/qr/resolve")
    public ApiResponse<JewelleryResponse> resolveQr(@Valid @RequestBody QrResolveRequest request) {
        return ApiResponse.success("QR resolved", jewelleryService.resolveQr(request.token()));
    }

    @PostMapping("/{id}/mark-found")
    public ApiResponse<JewelleryResponse> markFound(@PathVariable UUID id, @Valid @RequestBody MarkFoundRequest request) {
        return ApiResponse.success("Jewellery restored", jewelleryService.markFound(id, request));
    }
}
