package com.jwellkeeper.tenant.service;

import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.util.CurrencyValidator;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.tenant.dto.TenantSettingsResponse;
import com.jwellkeeper.tenant.dto.UpdateTenantSettingsRequest;
import com.jwellkeeper.tenant.model.TenantSettings;
import com.jwellkeeper.tenant.repository.TenantSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantSettingsService {

    public static final String DEFAULT_BILL_FORMAT = "{PREFIX}-{SEQUENCE:000000}";

    private final TenantSettingsRepository repository;
    private final BusinessLogService logService;

    @Transactional(readOnly = true)
    public TenantSettingsResponse getSettings() {
        return toResponse(getCurrentSettings());
    }

    @Transactional
    public TenantSettingsResponse update(UpdateTenantSettingsRequest request) {
        TenantSettings settings = getCurrentSettings();
        if (request.defaultCurrencyCode() != null) {
            settings.setDefaultCurrencyCode(CurrencyValidator.requireIsoCurrency(request.defaultCurrencyCode()));
        }
        if (request.billPrefix() != null && !request.billPrefix().isBlank()) {
            settings.setBillPrefix(request.billPrefix().trim().toUpperCase());
        }
        if (request.billNumberFormat() != null && !request.billNumberFormat().isBlank()) {
            settings.setBillNumberFormat(request.billNumberFormat().trim());
        }
        if (request.nextBillSequence() != null) {
            if (request.nextBillSequence() < 1) {
                throw new BadRequestException("nextBillSequence must be greater than zero");
            }
            settings.setNextBillSequence(request.nextBillSequence());
        }
        logService.log("TENANT_SETTINGS_UPDATED", "TenantSettings", settings.getTenantId(), "SUCCESS", "Tenant settings updated", Map.of());
        return toResponse(settings);
    }

    public TenantSettings getCurrentSettings() {
        return repository.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant settings not found"));
    }

    public TenantSettingsResponse toResponse(TenantSettings settings) {
        return new TenantSettingsResponse(
                settings.getDefaultCurrencyCode(),
                settings.getBillPrefix(),
                settings.getBillNumberFormat(),
                settings.getNextBillSequence(),
                settings.getSequenceResetPolicy().name()
        );
    }
}
