package com.jwellkeeper.jewellery.service;

import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.jewellery.dto.StockAdjustmentRequest;
import com.jwellkeeper.jewellery.dto.StockAdjustmentResponse;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.model.JewelleryType;
import com.jwellkeeper.jewellery.model.StockAdjustment;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.jewellery.repository.JewelleryTypeRepository;
import com.jwellkeeper.jewellery.repository.StockAdjustmentRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final JewelleryRepository jewelleryRepository;
    private final JewelleryTypeRepository typeRepository;
    private final AuthorizationService authorizationService;
    private final StockMovementService movementService;
    private final BusinessLogService logService;

    @Transactional
    public StockAdjustmentResponse adjust(UUID jewelleryId, StockAdjustmentRequest request) {
        authorizationService.validateOwnerOrManagerPassword(request.password(), "Owner or manager password is required for stock adjustments");
        if (request.typeId() == null && isBlank(request.karat()) && request.weight() == null && !Boolean.TRUE.equals(request.archive())) {
            throw new BadRequestException("Provide at least one stock-critical change");
        }
        UUID tenantId = TenantContext.requireTenantId();
        Jewellery jewellery = jewelleryRepository.lockByIdAndTenantId(jewelleryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        if (jewellery.getStatus() == JewelleryStatus.SOLD) {
            throw new ConflictException("Sold jewellery cannot be adjusted directly. Use bill correction flow.");
        }

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setTenantId(tenantId);
        adjustment.setJewelleryId(jewellery.getId());
        adjustment.setBeforeTypeId(jewellery.getTypeId());
        adjustment.setBeforeKarat(jewellery.getKarat());
        adjustment.setBeforeWeight(jewellery.getWeight());
        adjustment.setBeforeStatus(jewellery.getStatus());
        adjustment.setReason(request.reason().trim());
        adjustment.setCreatedBy(TenantContext.requireUser().userId());

        if (request.typeId() != null) {
            JewelleryType type = typeRepository.findByIdAndTenantId(request.typeId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Jewellery type not found"));
            jewellery.setTypeId(type.getId());
            jewellery.setType(type);
        }
        if (!isBlank(request.karat())) {
            jewellery.setKarat(normalizeKarat(request.karat()));
        }
        if (request.weight() != null) {
            jewellery.setWeight(request.weight());
        }
        if (Boolean.TRUE.equals(request.archive())) {
            jewellery.setStatus(JewelleryStatus.ARCHIVED);
            jewellery.setDeletedAt(Instant.now());
        }

        adjustment.setAfterTypeId(jewellery.getTypeId());
        adjustment.setAfterKarat(jewellery.getKarat());
        adjustment.setAfterWeight(jewellery.getWeight());
        adjustment.setAfterStatus(jewellery.getStatus());
        adjustmentRepository.save(adjustment);

        movementService.record(
                jewellery,
                Boolean.TRUE.equals(request.archive()) ? StockMovementType.ARCHIVED : StockMovementType.STOCK_ADJUSTED,
                "StockAdjustment",
                adjustment.getId(),
                adjustment.getBeforeStatus(),
                adjustment.getAfterStatus(),
                request.reason().trim(),
                Map.of(
                        "beforeTypeId", nullSafe(adjustment.getBeforeTypeId()),
                        "afterTypeId", nullSafe(adjustment.getAfterTypeId()),
                        "beforeKarat", nullSafe(adjustment.getBeforeKarat()),
                        "afterKarat", nullSafe(adjustment.getAfterKarat()),
                        "beforeWeight", nullSafe(adjustment.getBeforeWeight()),
                        "afterWeight", nullSafe(adjustment.getAfterWeight())
                )
        );
        logService.log("STOCK_ADJUSTED", "Jewellery", jewellery.getId(), "SUCCESS", "Stock adjustment recorded", Map.of("reason", request.reason()));
        return toResponse(adjustment);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentResponse> list(UUID jewelleryId, Pageable pageable) {
        return PageResponse.from(adjustmentRepository.findByTenantIdAndJewelleryIdOrderByCreatedAtDesc(TenantContext.requireTenantId(), jewelleryId, pageable).map(this::toResponse));
    }

    private StockAdjustmentResponse toResponse(StockAdjustment adjustment) {
        return new StockAdjustmentResponse(
                adjustment.getId(),
                adjustment.getJewelleryId(),
                adjustment.getBeforeTypeId(),
                adjustment.getAfterTypeId(),
                adjustment.getBeforeKarat(),
                adjustment.getAfterKarat(),
                adjustment.getBeforeWeight(),
                adjustment.getAfterWeight(),
                adjustment.getBeforeStatus(),
                adjustment.getAfterStatus(),
                adjustment.getReason(),
                adjustment.getCreatedBy(),
                adjustment.getCreatedAt()
        );
    }

    private String normalizeKarat(String karat) {
        String normalized = karat.trim().toUpperCase();
        if (!normalized.matches("^[0-9]{1,2}K$")) {
            throw new BadRequestException("karat must look like 24K, 22K, or 18K");
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
