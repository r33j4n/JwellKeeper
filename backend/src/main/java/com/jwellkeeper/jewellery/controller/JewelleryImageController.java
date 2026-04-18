package com.jwellkeeper.jewellery.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.jewellery.dto.ImageReorderRequest;
import com.jwellkeeper.jewellery.dto.JewelleryImageResponse;
import com.jwellkeeper.jewellery.model.ImageCaptureSource;
import com.jwellkeeper.jewellery.service.JewelleryImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jewellery/{jewelleryId}/images")
public class JewelleryImageController {

    private final JewelleryImageService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JewelleryImageResponse> upload(
            @PathVariable UUID jewelleryId,
            @RequestParam(required = false) ImageCaptureSource captureSource,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success("Jewellery image uploaded", service.upload(jewelleryId, captureSource, file));
    }

    @GetMapping
    public ApiResponse<List<JewelleryImageResponse>> list(@PathVariable UUID jewelleryId) {
        return ApiResponse.success("Jewellery images fetched", service.list(jewelleryId));
    }

    @PatchMapping("/{imageId}/primary")
    public ApiResponse<JewelleryImageResponse> primary(@PathVariable UUID jewelleryId, @PathVariable UUID imageId) {
        return ApiResponse.success("Primary image updated", service.markPrimary(jewelleryId, imageId));
    }

    @PatchMapping("/reorder")
    public ApiResponse<List<JewelleryImageResponse>> reorder(@PathVariable UUID jewelleryId, @Valid @org.springframework.web.bind.annotation.RequestBody ImageReorderRequest request) {
        return ApiResponse.success("Jewellery images reordered", service.reorder(jewelleryId, request));
    }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> delete(@PathVariable UUID jewelleryId, @PathVariable UUID imageId) {
        service.delete(jewelleryId, imageId);
        return ApiResponse.success("Jewellery image deleted");
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID jewelleryId, @PathVariable UUID imageId) {
        JewelleryImageService.StoredImage image = service.loadContent(jewelleryId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.filename() + "\"")
                .body(image.resource());
    }
}
