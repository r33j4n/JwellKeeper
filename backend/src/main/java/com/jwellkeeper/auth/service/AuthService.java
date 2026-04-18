package com.jwellkeeper.auth.service;

import com.jwellkeeper.auth.dto.AuthResponse;
import com.jwellkeeper.auth.dto.LoginRequest;
import com.jwellkeeper.auth.dto.RegisterRequest;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.UnauthorizedException;
import com.jwellkeeper.common.util.CurrencyValidator;
import com.jwellkeeper.jewellery.service.JewelleryTypeService;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.JwtService;
import com.jwellkeeper.tenant.model.SequenceResetPolicy;
import com.jwellkeeper.tenant.model.Tenant;
import com.jwellkeeper.tenant.model.TenantSettings;
import com.jwellkeeper.tenant.repository.TenantRepository;
import com.jwellkeeper.tenant.repository.TenantSettingsRepository;
import com.jwellkeeper.tenant.service.TenantSettingsService;
import com.jwellkeeper.users.model.UserAccount;
import com.jwellkeeper.users.model.UserRole;
import com.jwellkeeper.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JewelleryTypeService jewelleryTypeService;
    private final BusinessLogService logService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        Tenant tenant = new Tenant();
        tenant.setShopName(request.shopName().trim());
        tenantRepository.save(tenant);

        TenantSettings settings = new TenantSettings();
        settings.setTenantId(tenant.getId());
        settings.setDefaultCurrencyCode(CurrencyValidator.requireIsoCurrency(request.defaultCurrencyCode()));
        settings.setBillPrefix(request.billPrefix().trim().toUpperCase());
        settings.setBillNumberFormat(TenantSettingsService.DEFAULT_BILL_FORMAT);
        settings.setNextBillSequence(1);
        settings.setSequenceResetPolicy(SequenceResetPolicy.NEVER);
        tenantSettingsRepository.save(settings);

        UserAccount owner = new UserAccount();
        owner.setTenantId(tenant.getId());
        owner.setName(request.ownerName().trim());
        owner.setEmail(request.email().trim().toLowerCase());
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        userRepository.save(owner);

        jewelleryTypeService.createDefaults(tenant.getId());
        logService.logForTenant(tenant.getId(), owner.getId(), "TENANT_REGISTERED", "Tenant", tenant.getId(), "SUCCESS", "Tenant registered", Map.of("shopName", tenant.getShopName()));
        return toAuthResponse(owner);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        logService.logForTenant(user.getTenantId(), user.getId(), "LOGIN", "User", user.getId(), "SUCCESS", "User logged in", Map.of("email", user.getEmail()));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        String shopName = tenantRepository.findById(user.getTenantId())
                .map(Tenant::getShopName)
                .orElse("JwellKeeper");
        return new AuthResponse(
                jwtService.generate(user),
                user.getId(),
                user.getTenantId(),
                user.getRole(),
                user.getEmail(),
                user.getName(),
                shopName
        );
    }
}
