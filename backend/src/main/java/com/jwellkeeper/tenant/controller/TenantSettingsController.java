package com.jwellkeeper.tenant.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.tenant.dto.TenantSettingsResponse;
import com.jwellkeeper.tenant.dto.UpdateTenantSettingsRequest;
import com.jwellkeeper.tenant.service.TenantSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tenant/settings")
public class TenantSettingsController {

    private final TenantSettingsService service;

    @GetMapping
    public ApiResponse<TenantSettingsResponse> get() {
        return ApiResponse.success("Tenant settings fetched", service.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<TenantSettingsResponse> update(@Valid @RequestBody UpdateTenantSettingsRequest request) {
        return ApiResponse.success("Tenant settings updated", service.update(request));
    }
}
