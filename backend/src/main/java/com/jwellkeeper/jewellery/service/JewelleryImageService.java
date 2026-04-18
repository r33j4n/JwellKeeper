package com.jwellkeeper.jewellery.service;

import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.jewellery.dto.ImageReorderRequest;
import com.jwellkeeper.jewellery.dto.JewelleryImageResponse;
import com.jwellkeeper.jewellery.model.ImageCaptureSource;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryImage;
import com.jwellkeeper.jewellery.repository.JewelleryImageRepository;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JewelleryImageService {

    private static final long MAX_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_ACTIVE_IMAGES = 8;

    private final JewelleryImageRepository imageRepository;
    private final JewelleryRepository jewelleryRepository;
    private final StockMovementService movementService;
    private final BusinessLogService logService;

    @Value("${app.storage.root:storage}")
    private String storageRoot;

    @Transactional
    public JewelleryImageResponse upload(UUID jewelleryId, ImageCaptureSource captureSource, MultipartFile file) {
        UUID tenantId = TenantContext.requireTenantId();
        Jewellery jewellery = jewelleryRepository.findByIdAndTenantIdAndDeletedAtIsNull(jewelleryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image file must be 10 MB or smaller");
        }
        if (imageRepository.countByTenantIdAndJewelleryIdAndDeletedAtIsNull(tenantId, jewelleryId) >= MAX_ACTIVE_IMAGES) {
            throw new BadRequestException("A jewellery item can have at most 8 active images");
        }

        byte[] bytes = readBytes(file);
        ImageType imageType = detectImageType(bytes);
        ImageDimensions dimensions = readDimensions(bytes);
        String checksum = sha256(bytes);

        JewelleryImage image = new JewelleryImage();
        image.setTenantId(tenantId);
        image.setJewelleryId(jewelleryId);
        image.setCaptureSource(captureSource == null ? ImageCaptureSource.UPLOAD : captureSource);
        image.setMimeType(imageType.mimeType());
        image.setFileSizeBytes(bytes.length);
        image.setWidth(dimensions.width());
        image.setHeight(dimensions.height());
        image.setChecksumSha256(checksum);
        image.setCreatedBy(TenantContext.requireUser().userId());

        List<JewelleryImage> activeImages = imageRepository.findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(tenantId, jewelleryId);
        image.setSortOrder(activeImages.size());
        image.setPrimary(activeImages.isEmpty());
        image.setStorageKey("tenant/" + tenantId + "/jewellery/" + jewelleryId + "/" + image.getId() + "." + imageType.extension());
        imageRepository.save(image);
        writeFile(image.getStorageKey(), bytes);

        movementService.record(jewellery, StockMovementType.IMAGE_ADDED, "JewelleryImage", image.getId(), jewellery.getStatus(), jewellery.getStatus(), "Jewellery image added", Map.of("imageId", image.getId().toString()));
        logService.log("JEWELLERY_IMAGE_ADDED", "Jewellery", jewelleryId, "SUCCESS", "Jewellery image added", Map.of("imageId", image.getId().toString()));
        return toResponse(image);
    }

    @Transactional(readOnly = true)
    public List<JewelleryImageResponse> list(UUID jewelleryId) {
        UUID tenantId = TenantContext.requireTenantId();
        return imageRepository.findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(tenantId, jewelleryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public JewelleryImageResponse markPrimary(UUID jewelleryId, UUID imageId) {
        UUID tenantId = TenantContext.requireTenantId();
        JewelleryImage selected = imageRepository.findByIdAndTenantIdAndJewelleryIdAndDeletedAtIsNull(imageId, tenantId, jewelleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery image not found"));
        imageRepository.findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(tenantId, jewelleryId)
                .forEach(image -> image.setPrimary(image.getId().equals(selected.getId())));
        logService.log("JEWELLERY_IMAGE_PRIMARY_CHANGED", "Jewellery", jewelleryId, "SUCCESS", "Primary jewellery image changed", Map.of("imageId", imageId.toString()));
        return toResponse(selected);
    }

    @Transactional
    public List<JewelleryImageResponse> reorder(UUID jewelleryId, ImageReorderRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        List<JewelleryImage> activeImages = imageRepository.findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(tenantId, jewelleryId);
        Map<UUID, JewelleryImage> byId = activeImages.stream().collect(java.util.stream.Collectors.toMap(JewelleryImage::getId, image -> image));
        int index = 0;
        for (UUID imageId : request.imageIds()) {
            JewelleryImage image = byId.get(imageId);
            if (image == null) {
                throw new ResourceNotFoundException("Jewellery image not found: " + imageId);
            }
            image.setSortOrder(index++);
        }
        logService.log("JEWELLERY_IMAGES_REORDERED", "Jewellery", jewelleryId, "SUCCESS", "Jewellery images reordered", Map.of("images", request.imageIds().size()));
        return activeImages.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID jewelleryId, UUID imageId) {
        UUID tenantId = TenantContext.requireTenantId();
        Jewellery jewellery = jewelleryRepository.findByIdAndTenantIdAndDeletedAtIsNull(jewelleryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        JewelleryImage image = imageRepository.findByIdAndTenantIdAndJewelleryIdAndDeletedAtIsNull(imageId, tenantId, jewelleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery image not found"));
        image.setDeletedAt(Instant.now());
        image.setDeletedBy(TenantContext.requireUser().userId());
        if (image.isPrimary()) {
            imageRepository.findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(tenantId, jewelleryId)
                    .stream()
                    .filter(next -> !next.getId().equals(image.getId()))
                    .findFirst()
                    .ifPresent(next -> next.setPrimary(true));
        }
        movementService.record(jewellery, StockMovementType.IMAGE_REMOVED, "JewelleryImage", image.getId(), jewellery.getStatus(), jewellery.getStatus(), "Jewellery image removed", Map.of("imageId", image.getId().toString()));
        logService.log("JEWELLERY_IMAGE_REMOVED", "Jewellery", jewelleryId, "SUCCESS", "Jewellery image removed", Map.of("imageId", imageId.toString()));
    }

    @Transactional(readOnly = true)
    public StoredImage loadContent(UUID jewelleryId, UUID imageId) {
        UUID tenantId = TenantContext.requireTenantId();
        JewelleryImage image = imageRepository.findByIdAndTenantIdAndJewelleryIdAndDeletedAtIsNull(imageId, tenantId, jewelleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery image not found"));
        Path path = resolveStoragePath(image.getStorageKey());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Stored image file not found");
            }
            return new StoredImage(resource, image.getMimeType(), "jewellery-" + jewelleryId + "-" + image.getId());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Stored image file not found");
        }
    }

    private JewelleryImageResponse toResponse(JewelleryImage image) {
        return new JewelleryImageResponse(
                image.getId(),
                image.getJewelleryId(),
                image.getCaptureSource(),
                image.getMimeType(),
                image.getFileSizeBytes(),
                image.getWidth(),
                image.getHeight(),
                image.getChecksumSha256(),
                image.getSortOrder(),
                image.isPrimary(),
                "/api/jewellery/" + image.getJewelleryId() + "/images/" + image.getId() + "/content",
                image.getCreatedAt()
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded image");
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
        throw new BadRequestException("Only JPG, PNG, and WEBP images are supported");
    }

    private ImageDimensions readDimensions(byte[] bytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            var image = ImageIO.read(input);
            if (image == null) {
                return new ImageDimensions(null, null);
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        } catch (IOException ignored) {
            return new ImageDimensions(null, null);
        }
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
            throw new BadRequestException("Could not store uploaded image");
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

    private record ImageDimensions(Integer width, Integer height) {
    }

    public record StoredImage(Resource resource, String mimeType, String filename) {
    }
}
