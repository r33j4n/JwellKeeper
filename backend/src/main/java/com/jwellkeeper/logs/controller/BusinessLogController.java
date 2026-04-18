package com.jwellkeeper.logs.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.logs.dto.BusinessLogResponse;
import com.jwellkeeper.logs.model.BusinessLog;
import com.jwellkeeper.logs.repository.BusinessLogRepository;
import com.jwellkeeper.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logs")
public class BusinessLogController {

    private final BusinessLogRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PageResponse<BusinessLogResponse>> list(Pageable pageable) {
        return ApiResponse.success("Logs fetched", PageResponse.from(
                repository.findByTenantId(TenantContext.requireTenantId(), pageable).map(this::toResponse)
        ));
    }

    private BusinessLogResponse toResponse(BusinessLog log) {
        return new BusinessLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getResult(),
                log.getMessage(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
