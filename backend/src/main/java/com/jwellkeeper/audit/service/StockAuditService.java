package com.jwellkeeper.audit.service;

import com.jwellkeeper.audit.dto.AuditCloseRequest;
import com.jwellkeeper.audit.dto.AuditAttemptCloseResponse;
import com.jwellkeeper.audit.dto.AuditForceCloseRequest;
import com.jwellkeeper.audit.dto.AuditReportCurrencyTotal;
import com.jwellkeeper.audit.dto.AuditReportResponse;
import com.jwellkeeper.audit.dto.AuditReportSoldRow;
import com.jwellkeeper.audit.dto.AuditReportStockRow;
import com.jwellkeeper.audit.dto.AuditReportTypeTally;
import com.jwellkeeper.audit.dto.AuditScanRequest;
import com.jwellkeeper.audit.dto.AuditScanResponse;
import com.jwellkeeper.audit.dto.StartAuditRequest;
import com.jwellkeeper.audit.dto.StockAuditResponse;
import com.jwellkeeper.audit.mapper.StockAuditMapper;
import com.jwellkeeper.audit.model.AuditItemResolution;
import com.jwellkeeper.audit.model.StockAudit;
import com.jwellkeeper.audit.model.StockAuditItem;
import com.jwellkeeper.audit.model.StockAuditStage;
import com.jwellkeeper.audit.model.StockAuditStatus;
import com.jwellkeeper.audit.repository.StockAuditItemRepository;
import com.jwellkeeper.audit.repository.StockAuditRepository;
import com.jwellkeeper.billing.model.BillItem;
import com.jwellkeeper.billing.repository.BillItemRepository;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ForbiddenException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.jewellery.dto.QrPayload;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.jewellery.service.QrCodeService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockAuditService {

    private final StockAuditRepository auditRepository;
    private final StockAuditItemRepository itemRepository;
    private final JewelleryRepository jewelleryRepository;
    private final BillItemRepository billItemRepository;
    private final QrCodeService qrCodeService;
    private final StockAuditMapper mapper;
    private final BusinessLogService logService;
    private final AuditPdfService pdfService;
    private final AuthorizationService authorizationService;
    private final ShopStateService shopStateService;
    private final StockMovementService movementService;

    @Transactional
    public StockAuditResponse start(StartAuditRequest request) {
        if (request == null) {
            request = new StartAuditRequest(null, null, null);
        }
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate auditDate = request.auditDate() == null ? LocalDate.now() : request.auditDate();
        if (!shopStateService.isClosed(tenantId, auditDate)) {
            throw new ConflictException("Shop must be closed before starting stock audit");
        }
        if (auditRepository.existsOpenAudit(tenantId)) {
            throw new ConflictException("Another stock audit is already open");
        }
        int runNumber = auditRepository.maxRunNumberForDate(tenantId, auditDate) + 1;
        StockAudit previousAudit = auditRepository.findTopByTenantIdAndAuditDateOrderByRunNumberDesc(tenantId, auditDate).orElse(null);
        if (runNumber > 1) {
            authorizationService.validateOwnerOrManagerPassword(request.password(), "Owner or manager password is required to start " + auditName(auditDate, runNumber));
            if (isBlank(request.repeatReason())) {
                throw new BadRequestException("Repeat audit reason is required");
            }
        } else {
            authorizationService.requireAny(UserRole.OWNER, UserRole.MANAGER, UserRole.STOCK_KEEPER, UserRole.STAFF);
        }

        var currentUser = TenantContext.requireUser();
        StockAudit audit = new StockAudit();
        audit.setTenantId(tenantId);
        audit.setAuditDate(auditDate);
        audit.setRunNumber(runNumber);
        audit.setStatus(StockAuditStatus.OPEN);
        audit.setStage(StockAuditStage.SCANNING);
        audit.setStartedAt(Instant.now());
        audit.setStartedBy(currentUser.userId());
        audit.setRepeatReason(normalizeOptionalText(request.repeatReason()));
        audit.setRepeatOfAuditId(previousAudit == null ? null : previousAudit.getId());

        List<Jewellery> available = jewelleryRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, JewelleryStatus.AVAILABLE);
        audit.setExpectedCount(available.size());
        audit.setExpectedTotalWeight(available.stream()
                .map(Jewellery::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        available.forEach(jewellery -> {
            StockAuditItem item = new StockAuditItem();
            item.setTenantId(tenantId);
            item.setJewelleryId(jewellery.getId());
            item.setScanned(false);
            item.setResolution(AuditItemResolution.PENDING);
            audit.addItem(item);
        });
        auditRepository.save(audit);
        logService.log(
                "AUDIT_STARTED",
                "StockAudit",
                audit.getId(),
                "SUCCESS",
                auditName(audit) + " started",
                Map.of(
                        "auditDate", auditDate.toString(),
                        "runNumber", runNumber,
                        "auditName", auditName(audit),
                        "items", available.size(),
                        "expectedWeight", audit.getExpectedTotalWeight(),
                        "repeatAudit", runNumber > 1,
                        "repeatReason", nullSafe(audit.getRepeatReason())
                )
        );
        return toResponse(audit);
    }

    @Transactional
    public AuditScanResponse scan(AuditScanRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        StockAudit audit = auditRepository.lockByIdAndTenantId(request.auditId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
        if (audit.getStatus() != StockAuditStatus.OPEN) {
            throw new ConflictException("Audit is already closed");
        }
        QrPayload payload = qrCodeService.verify(request.token());
        if (!tenantId.equals(payload.tenantId())) {
            throw new ForbiddenException("QR code belongs to another tenant");
        }
        StockAuditItem item = itemRepository.findByAuditIdAndTenantIdAndJewelleryId(audit.getId(), tenantId, payload.jewelleryId())
                .orElseThrow(() -> new ResourceNotFoundException("Scanned jewellery is not part of this audit"));
        if (item.isScanned() || item.getResolution() == AuditItemResolution.FOUND_IN_AUDIT) {
            return new AuditScanResponse(toResponse(audit), mapper.toItemResponse(item), true, "This jewellery was already scanned");
        }
        Instant now = Instant.now();
        item.setScanned(true);
        item.setScannedAt(now);
        item.setScannedBy(TenantContext.requireUser().userId());
        item.setResolution(AuditItemResolution.FOUND_IN_AUDIT);
        item.setResolutionChangedAt(now);
        jewelleryRepository.findByIdAndTenantIdAndDeletedAtIsNull(payload.jewelleryId(), tenantId)
                .ifPresent(jewellery -> movementService.record(
                        jewellery,
                        StockMovementType.AUDIT_SCANNED,
                        "StockAudit",
                        audit.getId(),
                        jewellery.getStatus(),
                        jewellery.getStatus(),
                        "Scanned during " + auditName(audit),
                        Map.of("auditName", auditName(audit), "runNumber", audit.getRunNumber())
                ));
        logService.log("AUDIT_ITEM_SCANNED", "StockAudit", audit.getId(), "SUCCESS", auditName(audit) + " item scanned", Map.of("jewelleryId", payload.jewelleryId().toString(), "auditName", auditName(audit), "runNumber", audit.getRunNumber()));
        return new AuditScanResponse(toResponse(audit), mapper.toItemResponse(item), false, "Audit item scanned");
    }

    @Transactional
    public StockAuditResponse close(AuditCloseRequest request) {
        AuditAttemptCloseResponse attempt = attemptCloseInternal(request.auditId());
        if (attempt.canCloseCleanly()) {
            return attempt.audit();
        }
        if (isBlank(request.ownerPassword())) {
            throw new BadRequestException("Owner or manager password is required to force close unresolved audit items");
        }
        String reason = isBlank(request.reason()) ? "Forced through legacy close endpoint" : request.reason();
        return forceCloseInternal(request.auditId(), request.ownerPassword(), reason);
    }

    @Transactional
    public AuditAttemptCloseResponse attemptClose(UUID auditId) {
        return attemptCloseInternal(auditId);
    }

    @Transactional
    public StockAuditResponse forceClose(UUID auditId, AuditForceCloseRequest request) {
        return forceCloseInternal(auditId, request.password(), request.reason());
    }

    private AuditAttemptCloseResponse attemptCloseInternal(UUID auditId) {
        UUID tenantId = TenantContext.requireTenantId();
        StockAudit audit = auditRepository.lockByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
        if (audit.getStatus() != StockAuditStatus.OPEN) {
            throw new ConflictException("Audit is already closed");
        }

        List<StockAuditItem> unresolved = itemRepository.findByAuditIdAndTenantIdAndResolution(audit.getId(), tenantId, AuditItemResolution.PENDING);
        if (!unresolved.isEmpty()) {
            audit.setStage(StockAuditStage.SEARCHING_UNRESOLVED);
            logService.log("AUDIT_CLOSE_BLOCKED_UNRESOLVED", "StockAudit", audit.getId(), "SUCCESS", auditName(audit) + " has unresolved items", Map.of("unresolvedItems", unresolved.size(), "auditName", auditName(audit), "runNumber", audit.getRunNumber()));
            return new AuditAttemptCloseResponse(toResponse(audit), false, unresolved.stream().map(mapper::toItemResponse).toList());
        }

        closeCleanly(audit);
        logService.log("AUDIT_CLOSED", "StockAudit", audit.getId(), "SUCCESS", auditName(audit) + " closed cleanly", Map.of("missingItems", 0, "auditName", auditName(audit), "runNumber", audit.getRunNumber()));
        return new AuditAttemptCloseResponse(toResponse(audit), true, List.of());
    }

    private StockAuditResponse forceCloseInternal(UUID auditId, String password, String reason) {
        authorizationService.validateOwnerOrManagerPassword(password, "Owner or manager password is required to force close unresolved audit items");
        if (isBlank(reason)) {
            throw new BadRequestException("Force close reason is required");
        }
        UUID tenantId = TenantContext.requireTenantId();
        StockAudit audit = auditRepository.lockByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
        if (audit.getStatus() != StockAuditStatus.OPEN) {
            throw new ConflictException("Audit is already closed");
        }
        List<StockAuditItem> unresolved = itemRepository.findByAuditIdAndTenantIdAndResolution(audit.getId(), tenantId, AuditItemResolution.PENDING);
        Instant now = Instant.now();
        audit.setManuallyClosed(true);
        audit.setForceClosed(!unresolved.isEmpty());
        audit.setForceClosedAt(now);
        audit.setForceClosedBy(TenantContext.requireUser().userId());
        audit.setForceCloseReason(reason.trim());
        for (StockAuditItem item : unresolved) {
            item.setResolution(AuditItemResolution.MARKED_MISSING_ON_CLOSE);
            item.setResolutionChangedAt(now);
            Jewellery jewellery = jewelleryRepository.lockByIdAndTenantId(item.getJewelleryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found"));
            if (jewellery.getStatus() == JewelleryStatus.AVAILABLE) {
                JewelleryStatus fromStatus = jewellery.getStatus();
                jewellery.setStatus(JewelleryStatus.MISSING);
                movementService.record(
                        jewellery,
                        StockMovementType.MISSING_CONFIRMED,
                        "StockAudit",
                        audit.getId(),
                        fromStatus,
                        jewellery.getStatus(),
                        reason.trim(),
                        Map.of("auditName", auditName(audit), "runNumber", audit.getRunNumber())
                );
            }
        }
        audit.setStatus(StockAuditStatus.CLOSED);
        audit.setStage(StockAuditStage.FINALIZED);
        audit.setClosedAt(Instant.now());
        audit.setClosedBy(TenantContext.requireUser().userId());
        logService.log("AUDIT_FORCE_CLOSED", "StockAudit", audit.getId(), "SUCCESS", auditName(audit) + " force closed", Map.of("missingItems", unresolved.size(), "auditName", auditName(audit), "runNumber", audit.getRunNumber(), "reason", reason.trim()));
        return toResponse(audit);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockAuditResponse> list(Pageable pageable) {
        return PageResponse.from(auditRepository.findByTenantIdOrderByAuditDateDescRunNumberDesc(TenantContext.requireTenantId(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditReportResponse report(UUID auditId) {
        StockAudit audit = findAudit(auditId);
        return buildReport(audit);
    }

    @Transactional
    public byte[] generatePdf(UUID auditId) {
        StockAudit audit = findAudit(auditId);
        AuditReportResponse report = buildReport(audit);
        logService.log("AUDIT_PDF_DOWNLOADED", "StockAudit", audit.getId(), "SUCCESS", auditName(audit) + " PDF generated", Map.of("auditDate", audit.getAuditDate().toString(), "auditName", auditName(audit), "runNumber", audit.getRunNumber()));
        return pdfService.generate(audit, report);
    }

    private StockAudit findAudit(UUID auditId) {
        return auditRepository.findByIdAndTenantId(auditId, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
    }

    private AuditReportResponse buildReport(StockAudit audit) {
        long expectedAfterSales = audit.getItems().size();
        long scanned = itemRepository.countByAuditIdAndTenantIdAndScannedTrue(audit.getId(), audit.getTenantId());
        long missing = itemRepository.countByAuditIdAndTenantIdAndScannedFalse(audit.getId(), audit.getTenantId());
        long soldToday = billItemRepository.totalItemsSold(audit.getTenantId(), audit.getAuditDate(), audit.getAuditDate());
        BigDecimal salesTotal = billItemRepository.totalSales(audit.getTenantId(), audit.getAuditDate(), audit.getAuditDate());
        List<AuditReportStockRow> stockRows = currentStockRows(audit);
        List<AuditReportSoldRow> soldRows = soldRows(audit);
        return new AuditReportResponse(
                audit.getId(),
                audit.getAuditDate(),
                audit.getRunNumber(),
                auditName(audit),
                expectedAfterSales + soldToday,
                expectedAfterSales,
                scanned,
                missing,
                soldToday,
                salesTotal,
                stockRows,
                soldRows,
                typeTallies(stockRows, soldRows),
                salesTotalsByCurrency(soldRows),
                "/api/audit/" + audit.getId() + "/pdf"
        );
    }

    private List<AuditReportStockRow> currentStockRows(StockAudit audit) {
        List<StockAuditItem> items = itemRepository.findByAuditIdAndTenantId(audit.getId(), audit.getTenantId());
        if (items.isEmpty()) {
            return List.of();
        }
        List<UUID> jewelleryIds = items.stream().map(StockAuditItem::getJewelleryId).distinct().toList();
        Map<UUID, Jewellery> jewelleryById = jewelleryRepository.findByTenantIdAndIdIn(audit.getTenantId(), jewelleryIds)
                .stream()
                .collect(Collectors.toMap(Jewellery::getId, Function.identity()));

        return items.stream()
                .map(item -> stockRow(audit, item, jewelleryById.get(item.getJewelleryId())))
                .sorted(Comparator
                        .comparing(AuditReportStockRow::category)
                        .thenComparing(row -> nullSafe(row.typeName()))
                        .thenComparing(row -> nullSafe(row.designName()))
                        .thenComparing(row -> nullSafe(row.karat())))
                .toList();
    }

    private AuditReportStockRow stockRow(StockAudit audit, StockAuditItem item, Jewellery jewellery) {
        boolean todayAdded = jewellery != null
                && jewellery.getCreatedAt() != null
                && LocalDate.ofInstant(jewellery.getCreatedAt(), ZoneId.systemDefault()).equals(audit.getAuditDate());
        String category = item.getResolution() == AuditItemResolution.MARKED_MISSING_ON_CLOSE || (jewellery != null && jewellery.getStatus() == JewelleryStatus.MISSING)
                ? "MISSING_IN_AUDIT"
                : todayAdded ? "TODAY_ADDED" : "ALREADY_AVAILABLE";
        return new AuditReportStockRow(
                item.getJewelleryId(),
                jewellery == null || jewellery.getType() == null ? "Unknown" : jewellery.getType().getName(),
                jewellery == null ? null : jewellery.getDesignName(),
                jewellery == null ? "-" : jewellery.getKarat(),
                jewellery == null ? BigDecimal.ZERO : jewellery.getWeight(),
                jewellery == null ? null : jewellery.getStatus(),
                item.isScanned(),
                item.getResolution(),
                jewellery == null ? null : jewellery.getCreatedAt(),
                category
        );
    }

    private List<AuditReportSoldRow> soldRows(StockAudit audit) {
        return billItemRepository.soldItemsForDate(audit.getTenantId(), audit.getAuditDate())
                .stream()
                .map(this::soldRow)
                .toList();
    }

    private AuditReportSoldRow soldRow(BillItem item) {
        return new AuditReportSoldRow(
                item.getJewelleryId(),
                item.getBill().getBillNo(),
                item.getBill().getBillDate(),
                item.getTypeNameSnapshot(),
                item.getDesignNameSnapshot(),
                item.getKaratSnapshot(),
                item.getWeight(),
                item.getFinalPrice(),
                item.getBill().getCurrencyCode(),
                item.getNotes()
        );
    }

    private List<AuditReportTypeTally> typeTallies(List<AuditReportStockRow> stockRows, List<AuditReportSoldRow> soldRows) {
        Map<String, TypeCounter> counters = new HashMap<>();
        stockRows.forEach(row -> {
            TypeCounter counter = counters.computeIfAbsent(nullSafe(row.typeName()), ignored -> new TypeCounter());
            if ("TODAY_ADDED".equals(row.category())) {
                counter.todayAddedCount++;
            } else if ("MISSING_IN_AUDIT".equals(row.category())) {
                counter.missingCount++;
            } else {
                counter.alreadyAvailableCount++;
            }
            counter.currentStockCount++;
            counter.currentStockWeight = counter.currentStockWeight.add(row.weight() == null ? BigDecimal.ZERO : row.weight());
        });
        soldRows.forEach(row -> counters.computeIfAbsent(nullSafe(row.typeName()), ignored -> new TypeCounter()).soldTodayCount++);
        return counters.entrySet().stream()
                .map(entry -> new AuditReportTypeTally(
                        entry.getKey(),
                        entry.getValue().todayAddedCount,
                        entry.getValue().alreadyAvailableCount,
                        entry.getValue().missingCount,
                        entry.getValue().currentStockCount,
                        entry.getValue().soldTodayCount,
                        entry.getValue().currentStockWeight
                ))
                .sorted(Comparator.comparing(AuditReportTypeTally::typeName))
                .toList();
    }

    private List<AuditReportCurrencyTotal> salesTotalsByCurrency(List<AuditReportSoldRow> soldRows) {
        Map<String, CurrencyCounter> counters = new HashMap<>();
        soldRows.forEach(row -> {
            CurrencyCounter counter = counters.computeIfAbsent(nullSafe(row.currencyCode()), ignored -> new CurrencyCounter());
            counter.itemCount++;
            counter.totalAmount = counter.totalAmount.add(row.finalPrice() == null ? BigDecimal.ZERO : row.finalPrice());
        });
        return counters.entrySet().stream()
                .map(entry -> new AuditReportCurrencyTotal(entry.getKey(), entry.getValue().totalAmount, entry.getValue().itemCount))
                .sorted(Comparator.comparing(AuditReportCurrencyTotal::currencyCode))
                .toList();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private StockAuditResponse toResponse(StockAudit audit) {
        long total = audit.getItems().size();
        long scanned = audit.getItems().stream().filter(StockAuditItem::isScanned).count();
        return new StockAuditResponse(
                audit.getId(),
                audit.getAuditDate(),
                audit.getRunNumber(),
                auditName(audit),
                audit.getStatus(),
                audit.getStage(),
                audit.isManuallyClosed(),
                audit.getClosedBy(),
                audit.getClosedAt(),
                audit.isForceClosed(),
                audit.getForceClosedBy(),
                audit.getForceClosedAt(),
                audit.getForceCloseReason(),
                audit.getRepeatReason(),
                audit.getRepeatOfAuditId(),
                audit.getExpectedCount(),
                audit.getExpectedTotalWeight(),
                total,
                scanned,
                total - scanned,
                audit.getItems().stream().map(mapper::toItemResponse).toList(),
                "/api/audit/" + audit.getId() + "/pdf"
        );
    }

    private String auditName(StockAudit audit) {
        return auditName(audit.getAuditDate(), audit.getRunNumber());
    }

    private String auditName(LocalDate auditDate, int runNumber) {
        return auditDate + " Audit #" + runNumber;
    }

    private void closeCleanly(StockAudit audit) {
        audit.setStatus(StockAuditStatus.CLOSED);
        audit.setStage(StockAuditStage.FINALIZED);
        audit.setClosedAt(Instant.now());
        audit.setClosedBy(TenantContext.requireUser().userId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeOptionalText(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static class TypeCounter {
        long todayAddedCount;
        long alreadyAvailableCount;
        long missingCount;
        long currentStockCount;
        long soldTodayCount;
        BigDecimal currentStockWeight = BigDecimal.ZERO;
    }

    private static class CurrencyCounter {
        long itemCount;
        BigDecimal totalAmount = BigDecimal.ZERO;
    }
}
