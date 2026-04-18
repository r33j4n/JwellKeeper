package com.jwellkeeper.billing.service;

import com.jwellkeeper.billing.dto.BillResponse;
import com.jwellkeeper.billing.dto.CreateBillRequest;
import com.jwellkeeper.billing.dto.WhatsAppSendResponse;
import com.jwellkeeper.billing.mapper.BillMapper;
import com.jwellkeeper.billing.model.Bill;
import com.jwellkeeper.billing.model.BillItem;
import com.jwellkeeper.billing.model.BillStatus;
import com.jwellkeeper.billing.repository.BillRepository;
import com.jwellkeeper.audit.repository.StockAuditRepository;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.common.util.CurrencyValidator;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.shop.service.ShopStateService;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.service.StockMovementService;
import com.jwellkeeper.tenant.service.TenantSettingsService;
import com.jwellkeeper.users.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository billRepository;
    private final JewelleryRepository jewelleryRepository;
    private final BillNumberService billNumberService;
    private final TenantSettingsService tenantSettingsService;
    private final BillMapper mapper;
    private final BillPdfService pdfService;
    private final BusinessLogService logService;
    private final StockAuditRepository auditRepository;
    private final ShopStateService shopStateService;
    private final AuthorizationService authorizationService;
    private final StockMovementService movementService;

    @Transactional
    public BillResponse create(CreateBillRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        authorizationService.requireAny(UserRole.OWNER, UserRole.MANAGER, UserRole.CASHIER, UserRole.STAFF);
        LocalDate billDate = request.billDate() == null ? LocalDate.now() : request.billDate();
        if (shopStateService.isClosed(tenantId, billDate)) {
            throw new ConflictException("Billing is blocked because the shop is closed for " + billDate);
        }
        if (auditRepository.existsOpenAudit(tenantId)) {
            throw new ConflictException("Billing is blocked while a stock audit is open");
        }
        Set<UUID> requestedJewellery = new HashSet<>();
        request.items().forEach(item -> {
            if (!requestedJewellery.add(item.jewelleryId())) {
                throw new ConflictException("The same jewellery item cannot be billed twice");
            }
        });

        String currencyCode = request.currencyCode() == null || request.currencyCode().isBlank()
                ? tenantSettingsService.getCurrentSettings().getDefaultCurrencyCode()
                : CurrencyValidator.requireIsoCurrency(request.currencyCode());

        Bill bill = new Bill();
        bill.setTenantId(tenantId);
        bill.setBillNo(billNumberService.nextBillNumber(tenantId));
        bill.setBillDate(billDate);
        bill.setStatus(BillStatus.ACTIVE);
        bill.setCurrencyCode(currencyCode);
        bill.setCustomerName(request.customerName());
        bill.setCustomerPhone(request.customerPhone());
        bill.setCustomerAddress(request.customerAddress());
        bill.setPaymentMethod(request.paymentMethod());
        bill.setNotes(request.notes());
        bill.setCreatedBy(TenantContext.requireUser().userId());

        BigDecimal total = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            Jewellery jewellery = jewelleryRepository.lockByIdAndTenantId(itemRequest.jewelleryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found: " + itemRequest.jewelleryId()));
            if (jewellery.getStatus() != JewelleryStatus.AVAILABLE) {
                throw new ConflictException("Only available jewellery can be billed: " + jewellery.getId());
            }

            BillItem item = new BillItem();
            item.setTenantId(tenantId);
            item.setJewelleryId(jewellery.getId());
            item.setTypeNameSnapshot(jewellery.getType().getName());
            item.setDesignNameSnapshot(jewellery.getDesignName());
            item.setKaratSnapshot(jewellery.getKarat());
            item.setWeight(jewellery.getWeight());
            item.setFinalPrice(itemRequest.finalPrice());
            item.setRatePerGram(itemRequest.ratePerGram());
            item.setMakingCharge(itemRequest.makingCharge());
            item.setDiscountAmount(itemRequest.discountAmount());
            item.setTaxAmount(itemRequest.taxAmount());
            item.setNotes(itemRequest.notes());
            bill.addItem(item);

            total = total.add(itemRequest.finalPrice());
            JewelleryStatus fromStatus = jewellery.getStatus();
            jewellery.setStatus(JewelleryStatus.SOLD);
            jewellery.setSoldAt(Instant.now());
            movementService.record(
                    jewellery,
                    StockMovementType.SOLD,
                    "Bill",
                    bill.getId(),
                    fromStatus,
                    jewellery.getStatus(),
                    "Sold on bill " + bill.getBillNo(),
                    Map.of("billNo", bill.getBillNo(), "finalPrice", itemRequest.finalPrice())
            );
        }

        bill.setTotalAmount(total);
        billRepository.save(bill);
        logService.log("BILL_CREATED", "Bill", bill.getId(), "SUCCESS", "Bill created", Map.of("billNo", bill.getBillNo(), "total", total));
        return mapper.toResponse(bill);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> list(LocalDate from, LocalDate to, String q, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(billRepository.search(tenantId, from, to, toLike(q), pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public BillResponse get(UUID id) {
        return mapper.toResponse(findBill(id));
    }

    @Transactional
    public byte[] generatePdf(UUID id) {
        Bill bill = findBill(id);
        logService.log("BILL_PDF_DOWNLOADED", "Bill", bill.getId(), "SUCCESS", "Bill PDF generated", Map.of("billNo", bill.getBillNo()));
        return pdfService.generate(bill);
    }

    @Transactional
    public WhatsAppSendResponse sendWhatsApp(UUID id, String phoneNumber) {
        Bill bill = findBill(id);
        logService.log("BILL_WHATSAPP_STUB", "Bill", bill.getId(), "SUCCESS", "WhatsApp PDF send stub called", Map.of("phoneNumber", phoneNumber, "billNo", bill.getBillNo()));
        return new WhatsAppSendResponse("STUBBED", "WhatsApp provider is not configured yet. Bill PDF send was logged only.");
    }

    private Bill findBill(UUID id) {
        return billRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    private String toLike(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toLowerCase() + "%";
    }
}
