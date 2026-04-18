package com.jwellkeeper.jewellery.service;

import com.jwellkeeper.billing.repository.BillItemRepository;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ForbiddenException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.jewellery.dto.ArchivedJewelleryRequest;
import com.jwellkeeper.jewellery.dto.CreateJewelleryRequest;
import com.jwellkeeper.jewellery.dto.JewelleryResponse;
import com.jwellkeeper.jewellery.dto.MarkFoundRequest;
import com.jwellkeeper.jewellery.dto.QrImageResponse;
import com.jwellkeeper.jewellery.dto.QrPayload;
import com.jwellkeeper.jewellery.dto.UpdateJewelleryRequest;
import com.jwellkeeper.jewellery.mapper.JewelleryMapper;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.model.JewelleryType;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.jewellery.repository.JewelleryTypeRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.shop.service.ShopStateService;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.service.StockMovementService;
import com.jwellkeeper.users.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JewelleryService {

    private final JewelleryRepository repository;
    private final JewelleryTypeRepository typeRepository;
    private final JewelleryMapper mapper;
    private final QrCodeService qrCodeService;
    private final BillItemRepository billItemRepository;
    private final BusinessLogService logService;
    private final AuthorizationService authorizationService;
    private final StockMovementService movementService;
    private final ShopStateService shopStateService;

    @Transactional
    public JewelleryResponse create(CreateJewelleryRequest request) {
        authorizationService.requireAny(UserRole.OWNER, UserRole.MANAGER, UserRole.STOCK_KEEPER, UserRole.STAFF);
        UUID tenantId = TenantContext.requireTenantId();
        if (shopStateService.isClosed(tenantId, LocalDate.now())) {
            throw new ConflictException("Adding jewellery is blocked because the shop is currently closed");
        }
        JewelleryType type = typeRepository.findByIdAndTenantId(request.typeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery type not found"));

        Jewellery jewellery = new Jewellery();
        jewellery.setId(UUID.randomUUID());
        jewellery.setTenantId(tenantId);
        jewellery.setTypeId(type.getId());
        jewellery.setType(type);
        jewellery.setKarat(normalizeKarat(request.karat()));
        jewellery.setDesignName(normalizeOptionalText(request.designName()));
        jewellery.setWeight(request.weight());
        jewellery.setStatus(JewelleryStatus.AVAILABLE);
        jewellery.setQrPayloadToken(qrCodeService.createToken(new QrPayload(jewellery.getId(), tenantId)));
        jewellery.setNotes(normalizeOptionalText(request.notes()));
        repository.save(jewellery);
        movementService.record(
                jewellery,
                StockMovementType.ITEM_CREATED,
                "Jewellery",
                jewellery.getId(),
                null,
                jewellery.getStatus(),
                "Initial stock entry",
                Map.of("type", type.getName(), "karat", jewellery.getKarat(), "weight", jewellery.getWeight())
        );
        logService.log("JEWELLERY_CREATED", "Jewellery", jewellery.getId(), "SUCCESS", "Jewellery created", Map.of("type", type.getName(), "designName", nullSafe(jewellery.getDesignName())));
        return toResponse(jewellery);
    }

    @Transactional(readOnly = true)
    public PageResponse<JewelleryResponse> list(JewelleryStatus status, UUID typeId, String karat, String q, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (status == JewelleryStatus.ARCHIVED) {
            throw new ForbiddenException("Use the protected archived jewellery endpoint");
        }
        String normalizedKarat = karat == null || karat.isBlank() ? null : normalizeKarat(karat);
        var page = repository.search(tenantId, status, typeId, normalizedKarat, toLike(q), pageable);
        Map<UUID, BillRef> billRefs = findBillRefs(tenantId, page.getContent().stream().map(Jewellery::getId).toList());
        return PageResponse.from(page.map(jewellery -> toResponse(jewellery, billRefs.get(jewellery.getId()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<JewelleryResponse> archived(ArchivedJewelleryRequest request, Pageable pageable) {
        if (request == null) {
            throw new BadRequestException("Owner password is required");
        }
        authorizationService.validateOwnerPassword(request.ownerPassword(), "Owner password is required to view archived jewellery");
        UUID tenantId = TenantContext.requireTenantId();
        String normalizedKarat = request.karat() == null || request.karat().isBlank() ? null : normalizeKarat(request.karat());
        var page = repository.searchArchived(tenantId, request.typeId(), normalizedKarat, toLike(request.q()), pageable);
        Map<UUID, BillRef> billRefs = findBillRefs(tenantId, page.getContent().stream().map(Jewellery::getId).toList());
        return PageResponse.from(page.map(jewellery -> toResponse(jewellery, billRefs.get(jewellery.getId()))));
    }

    @Transactional(readOnly = true)
    public JewelleryResponse get(UUID id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public JewelleryResponse update(UUID id, UpdateJewelleryRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Jewellery jewellery = repository.lockByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        if (jewellery.getStatus() == JewelleryStatus.SOLD) {
            throw new ConflictException("Sold jewellery cannot be edited");
        }
        if (request.typeId() != null || request.weight() != null || (request.karat() != null && !request.karat().isBlank())) {
            throw new BadRequestException("Type, karat, and weight are stock-critical fields. Use the stock adjustment endpoint.");
        }
        if (request.designName() != null) {
            jewellery.setDesignName(normalizeOptionalText(request.designName()));
        }
        if (request.notes() != null) {
            jewellery.setNotes(normalizeOptionalText(request.notes()));
        }
        logService.log("JEWELLERY_UPDATED", "Jewellery", jewellery.getId(), "SUCCESS", "Jewellery updated", Map.of());
        return toResponse(jewellery);
    }

    @Transactional
    public void delete(UUID id) {
        authorizationService.requireOwnerOrManager();
        Jewellery jewellery = repository.lockByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        if (jewellery.getStatus() == JewelleryStatus.SOLD) {
            throw new ConflictException("Sold jewellery cannot be deleted");
        }
        jewellery.setDeletedAt(Instant.now());
        JewelleryStatus fromStatus = jewellery.getStatus();
        jewellery.setStatus(JewelleryStatus.ARCHIVED);
        movementService.record(jewellery, StockMovementType.ARCHIVED, "Jewellery", jewellery.getId(), fromStatus, jewellery.getStatus(), "Soft deleted from jewellery management", Map.of());
        logService.log("JEWELLERY_DELETED", "Jewellery", jewellery.getId(), "SUCCESS", "Jewellery soft deleted", Map.of());
    }

    @Transactional(readOnly = true)
    public QrImageResponse getQr(UUID id) {
        Jewellery jewellery = findExisting(id);
        return new QrImageResponse("image/png", qrCodeService.generateBase64Png(jewellery.getQrPayloadToken()));
    }

    @Transactional(readOnly = true)
    public JewelleryResponse resolveQr(String token) {
        QrPayload payload = qrCodeService.verify(token);
        UUID tenantId = TenantContext.requireTenantId();
        if (!tenantId.equals(payload.tenantId())) {
            throw new ForbiddenException("QR code belongs to another tenant");
        }
        return toResponse(repository.findByIdAndTenantIdAndDeletedAtIsNull(payload.jewelleryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found")));
    }

    @Transactional
    public JewelleryResponse markFound(UUID id, MarkFoundRequest request) {
        authorizationService.validateOwnerOrManagerPassword(request.ownerPassword(), "Owner or manager password is required to restore missing jewellery");
        Jewellery jewellery = repository.lockByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
        if (jewellery.getStatus() != JewelleryStatus.MISSING) {
            throw new ConflictException("Only missing jewellery can be restored");
        }
        JewelleryStatus fromStatus = jewellery.getStatus();
        jewellery.setStatus(JewelleryStatus.AVAILABLE);
        movementService.record(jewellery, StockMovementType.MISSING_RESTORED, "Jewellery", jewellery.getId(), fromStatus, jewellery.getStatus(), "Missing jewellery restored", Map.of());
        logService.log("JEWELLERY_FOUND", "Jewellery", jewellery.getId(), "SUCCESS", "Missing jewellery restored", Map.of());
        return toResponse(jewellery);
    }

    public Jewellery findExisting(UUID id) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
    }

    private String normalizeKarat(String karat) {
        String normalized = karat.trim().toUpperCase();
        if (!normalized.matches("^[0-9]{1,2}K$")) {
            throw new BadRequestException("karat must look like 24K, 22K, or 18K");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String toLike(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private JewelleryResponse toResponse(Jewellery jewellery) {
        return toResponse(jewellery, findBillRef(jewellery.getTenantId(), jewellery.getId()));
    }

    private JewelleryResponse toResponse(Jewellery jewellery, BillRef billRef) {
        JewelleryResponse response = mapper.toResponse(jewellery);
        return new JewelleryResponse(
                response.id(),
                response.typeId(),
                response.typeName(),
                response.karat(),
                response.designName(),
                response.weight(),
                response.status(),
                response.notes(),
                billRef == null ? null : billRef.billId(),
                billRef == null ? null : billRef.billNo(),
                response.createdAt(),
                response.soldAt(),
                response.deletedAt(),
                response.version()
        );
    }

    private BillRef findBillRef(UUID tenantId, UUID jewelleryId) {
        Map<UUID, BillRef> billRefs = findBillRefs(tenantId, List.of(jewelleryId));
        return billRefs.get(jewelleryId);
    }

    private Map<UUID, BillRef> findBillRefs(UUID tenantId, List<UUID> jewelleryIds) {
        if (jewelleryIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BillRef> billRefs = new HashMap<>();
        billItemRepository.billNumbersByJewelleryIds(tenantId, jewelleryIds)
                .forEach(row -> billRefs.put((UUID) row[0], new BillRef((UUID) row[1], (String) row[2])));
        return billRefs;
    }

    private record BillRef(UUID billId, String billNo) {}
}
