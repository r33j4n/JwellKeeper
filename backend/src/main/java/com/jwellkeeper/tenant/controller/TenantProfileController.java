package com.jwellkeeper.tenant.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.tenant.dto.TenantProfileResponse;
import com.jwellkeeper.tenant.dto.UpdateTenantProfileRequest;
import com.jwellkeeper.tenant.service.TenantProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tenant/profile")
public class TenantProfileController {

    private final TenantProfileService service;

    @GetMapping
    public ApiResponse<TenantProfileResponse> get() {
        return ApiResponse.success("Shop profile fetched", service.getProfile());
    }

    @PutMapping
    public ApiResponse<TenantProfileResponse> update(@Valid @RequestBody UpdateTenantProfileRequest request) {
        return ApiResponse.success("Shop profile updated", service.update(request));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TenantProfileResponse> uploadLogo(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Shop logo uploaded", service.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    public ApiResponse<TenantProfileResponse> deleteLogo() {
        return ApiResponse.success("Shop logo removed", service.deleteLogo());
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> logo() {
        TenantProfileService.StoredLogo logo = service.loadLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + logo.filename() + "\"")
                .body(logo.resource());
    }
}
