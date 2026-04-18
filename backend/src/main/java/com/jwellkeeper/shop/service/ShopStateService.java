package com.jwellkeeper.shop.service;

import com.jwellkeeper.audit.repository.StockAuditRepository;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.shop.dto.ShopStateResponse;
import com.jwellkeeper.shop.model.ShopDayStatus;
import com.jwellkeeper.shop.model.ShopStatus;
import com.jwellkeeper.shop.repository.ShopDayStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopStateService {

    private final ShopDayStatusRepository repository;
    private final StockAuditRepository auditRepository;
    private final AuthorizationService authorizationService;
    private final BusinessLogService logService;

    @Transactional(readOnly = true)
    public ShopStateResponse getState(LocalDate businessDate) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate date = normalizeDate(businessDate);
        return repository.findByTenantIdAndBusinessDate(tenantId, date)
                .map(this::toResponse)
                .orElseGet(() -> new ShopStateResponse(null, date, ShopStatus.OPEN, null, null, null, null, 0));
    }

    @Transactional
    public ShopStateResponse close(LocalDate businessDate) {
        authorizationService.requireOwnerOrManager();
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate date = normalizeDate(businessDate);
        ShopDayStatus state = getOrCreate(tenantId, date);
        state.setStatus(ShopStatus.CLOSED);
        state.setClosedBy(TenantContext.requireUser().userId());
        state.setClosedAt(Instant.now());
        repository.save(state);
        logService.log("SHOP_CLOSED", "ShopDayStatus", state.getId(), "SUCCESS", "Shop closed", Map.of("businessDate", date.toString()));
        return toResponse(state);
    }

    @Transactional
    public ShopStateResponse open(LocalDate businessDate) {
        authorizationService.requireOwnerOrManager();
        UUID tenantId = TenantContext.requireTenantId();
        if (auditRepository.existsOpenAudit(tenantId)) {
            throw new ConflictException("Cannot open shop while an audit is open");
        }
        LocalDate date = normalizeDate(businessDate);
        ShopDayStatus state = getOrCreate(tenantId, date);
        state.setStatus(ShopStatus.OPEN);
        state.setOpenedBy(TenantContext.requireUser().userId());
        state.setOpenedAt(Instant.now());
        repository.save(state);
        logService.log("SHOP_OPENED", "ShopDayStatus", state.getId(), "SUCCESS", "Shop opened", Map.of("businessDate", date.toString()));
        return toResponse(state);
    }

    @Transactional(readOnly = true)
    public boolean isClosed(UUID tenantId, LocalDate businessDate) {
        return repository.findByTenantIdAndBusinessDate(tenantId, normalizeDate(businessDate))
                .map(state -> state.getStatus() == ShopStatus.CLOSED)
                .orElse(false);
    }

    private ShopDayStatus getOrCreate(UUID tenantId, LocalDate date) {
        return repository.findByTenantIdAndBusinessDate(tenantId, date).orElseGet(() -> {
            ShopDayStatus state = new ShopDayStatus();
            state.setTenantId(tenantId);
            state.setBusinessDate(date);
            state.setStatus(ShopStatus.OPEN);
            return state;
        });
    }

    private LocalDate normalizeDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private ShopStateResponse toResponse(ShopDayStatus state) {
        return new ShopStateResponse(
                state.getId(),
                state.getBusinessDate(),
                state.getStatus(),
                state.getOpenedBy(),
                state.getOpenedAt(),
                state.getClosedBy(),
                state.getClosedAt(),
                state.getVersion()
        );
    }
}
