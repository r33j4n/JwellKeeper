package com.jwellkeeper.tenant.service;

import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.tenant.dto.TenantProfileResponse;
import com.jwellkeeper.tenant.dto.UpdateTenantProfileRequest;
import com.jwellkeeper.tenant.model.Tenant;
import com.jwellkeeper.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantProfileService {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;

    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;
    private final BusinessLogService logService;

    @Value("${app.storage.root:storage}")
    private String storageRoot;

    @Transactional(readOnly = true)
    public TenantProfileResponse getProfile() {
        return toResponse(currentTenant());
    }

    @Transactional
    public TenantProfileResponse update(UpdateTenantProfileRequest request) {
        authorizationService.requireAny(com.jwellkeeper.users.model.UserRole.OWNER);
        Tenant tenant = currentTenant();
        tenant.setShopName(request.shopName().trim());
        tenant.setShopAddress(normalize(request.shopAddress()));
        tenant.setShopContactNumber(normalize(request.shopContactNumber()));
        tenant.setShopEmail(normalize(request.shopEmail()));
        tenant.setTaxNumber(normalize(request.taxNumber()));
        tenant.setReceiptFooterNote(normalize(request.receiptFooterNote()));
        logService.log("TENANT_PROFILE_UPDATED", "Tenant", tenant.getId(), "SUCCESS", "Shop profile updated", Map.of("shopName", tenant.getShopName()));
        return toResponse(tenant);
    }

    @Transactional
    public TenantProfileResponse uploadLogo(MultipartFile file) {
        authorizationService.requireAny(com.jwellkeeper.users.model.UserRole.OWNER);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Logo file is required");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BadRequestException("Logo must be 2 MB or smaller");
        }
        byte[] bytes = readBytes(file);
        ImageType imageType = detectImageType(bytes);
        Tenant tenant = currentTenant();
        String storageKey = "tenant/" + tenant.getId() + "/profile/logo-" + sha256(bytes) + "." + imageType.extension();
        writeFile(storageKey, bytes);
        tenant.setLogoStorageKey(storageKey);
        tenant.setLogoMimeType(imageType.mimeType());
        logService.log("TENANT_LOGO_UPDATED", "Tenant", tenant.getId(), "SUCCESS", "Shop logo updated", Map.of("mimeType", imageType.mimeType()));
        return toResponse(tenant);
    }

    @Transactional
    public TenantProfileResponse deleteLogo() {
        authorizationService.requireAny(com.jwellkeeper.users.model.UserRole.OWNER);
        Tenant tenant = currentTenant();
        tenant.setLogoStorageKey(null);
        tenant.setLogoMimeType(null);
        logService.log("TENANT_LOGO_REMOVED", "Tenant", tenant.getId(), "SUCCESS", "Shop logo removed", Map.of());
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public StoredLogo loadLogo() {
        Tenant tenant = currentTenant();
        if (tenant.getLogoStorageKey() == null || tenant.getLogoMimeType() == null) {
            throw new ResourceNotFoundException("Shop logo not found");
        }
        Path path = resolveStoragePath(tenant.getLogoStorageKey());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Shop logo file not found");
            }
            return new StoredLogo(resource, tenant.getLogoMimeType(), "shop-logo");
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Shop logo file not found");
        }
    }

    public Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    public TenantProfileResponse toResponse(Tenant tenant) {
        boolean logoAvailable = tenant.getLogoStorageKey() != null;
        return new TenantProfileResponse(
                tenant.getId(),
                tenant.getShopName(),
                tenant.getShopAddress(),
                tenant.getShopContactNumber(),
                tenant.getShopEmail(),
                tenant.getTaxNumber(),
                tenant.getReceiptFooterNote(),
                logoAvailable,
                logoAvailable ? "/api/tenant/profile/logo" : null
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded logo");
        }
    }

    private ImageType detectImageType(byte[] bytes) {
        if (bytes.length >= 4 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) {
            return new ImageType("image/png", "png");
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return new ImageType("image/jpeg", "jpg");
        }
        if (bytes.length >= 12
                && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return new ImageType("image/webp", "webp");
        }
        throw new BadRequestException("Only PNG, JPG, and WEBP logos are supported");
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void writeFile(String storageKey, byte[] bytes) {
        Path path = resolveStoragePath(storageKey);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new BadRequestException("Could not store uploaded logo");
        }
    }

    private Path resolveStoragePath(String storageKey) {
        Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new BadRequestException("Invalid storage key");
        }
        return path;
    }

    private record ImageType(String mimeType, String extension) {
    }

    public record StoredLogo(Resource resource, String mimeType, String filename) {
    }
}
