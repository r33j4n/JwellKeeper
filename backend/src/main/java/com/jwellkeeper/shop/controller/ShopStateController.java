package com.jwellkeeper.shop.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.shop.dto.ShopStateChangeRequest;
import com.jwellkeeper.shop.dto.ShopStateResponse;
import com.jwellkeeper.shop.service.ShopStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop")
public class ShopStateController {

    private final ShopStateService service;

    @GetMapping("/state")
    public ApiResponse<ShopStateResponse> state(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success("Shop state fetched", service.getState(date));
    }

    @PostMapping("/close")
    public ApiResponse<ShopStateResponse> close(@RequestBody(required = false) ShopStateChangeRequest request) {
        return ApiResponse.success("Shop closed", service.close(request == null ? null : request.businessDate()));
    }

    @PostMapping("/open")
    public ApiResponse<ShopStateResponse> open(@RequestBody(required = false) ShopStateChangeRequest request) {
        return ApiResponse.success("Shop opened", service.open(request == null ? null : request.businessDate()));
    }
}
