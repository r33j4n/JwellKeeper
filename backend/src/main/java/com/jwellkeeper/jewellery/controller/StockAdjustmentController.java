package com.jwellkeeper.jewellery.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.jewellery.dto.StockAdjustmentRequest;
import com.jwellkeeper.jewellery.dto.StockAdjustmentResponse;
import com.jwellkeeper.jewellery.service.StockAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jewellery/{jewelleryId}/adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService service;

    @PostMapping
    public ApiResponse<StockAdjustmentResponse> adjust(@PathVariable UUID jewelleryId, @Valid @RequestBody StockAdjustmentRequest request) {
        return ApiResponse.success("Stock adjustment recorded", service.adjust(jewelleryId, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<StockAdjustmentResponse>> list(@PathVariable UUID jewelleryId, Pageable pageable) {
        return ApiResponse.success("Stock adjustments fetched", service.list(jewelleryId, pageable));
    }
}
