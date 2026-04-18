package com.jwellkeeper.billing.service;

import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.tenant.model.TenantSettings;
import com.jwellkeeper.tenant.repository.TenantSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BillNumberService {

    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("\\{SEQUENCE:(0+)\\}");

    private final TenantSettingsRepository settingsRepository;

    public String nextBillNumber(UUID tenantId) {
        TenantSettings settings = settingsRepository.lockByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant settings not found"));
        long sequence = settings.getNextBillSequence();
        settings.setNextBillSequence(sequence + 1);
        return format(settings.getBillNumberFormat(), settings.getBillPrefix(), sequence);
    }

    public String format(String format, String prefix, long sequence) {
        String resolved = format.replace("{PREFIX}", prefix);
        Matcher matcher = SEQUENCE_PATTERN.matcher(resolved);
        if (matcher.find()) {
            String zeros = matcher.group(1);
            return matcher.replaceFirst(String.format("%0" + zeros.length() + "d", sequence));
        }
        return resolved.replace("{SEQUENCE}", Long.toString(sequence));
    }
}
