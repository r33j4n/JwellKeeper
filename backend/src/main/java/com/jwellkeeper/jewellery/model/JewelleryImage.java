package com.jwellkeeper.jewellery.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jewellery_images")
public class JewelleryImage extends TenantScopedEntity {

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Column(name = "storage_key", nullable = false, length = 700)
    private String storageKey;

    @Column(name = "thumbnail_storage_key", length = 700)
    private String thumbnailStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_source", nullable = false, length = 20)
    private ImageCaptureSource captureSource;

    @Column(name = "mime_type", nullable = false, length = 80)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_by", nullable = false, columnDefinition = "char(36)")
    private UUID createdBy;

    @Column(name = "deleted_by", columnDefinition = "char(36)")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
