package com.jwellkeeper.billing.service;

import com.jwellkeeper.billing.dto.BillExchangeRequest;
import com.jwellkeeper.billing.dto.BillExchangeResponse;
import com.jwellkeeper.billing.dto.BillReturnRequest;
import com.jwellkeeper.billing.dto.BillReturnResponse;
import com.jwellkeeper.billing.dto.BillVoidRequest;
import com.jwellkeeper.billing.dto.BillVoidResponse;
import com.jwellkeeper.billing.dto.ReturnItemRequest;
import com.jwellkeeper.billing.model.Bill;
import com.jwellkeeper.billing.model.BillExchange;
import com.jwellkeeper.billing.model.BillItem;
import com.jwellkeeper.billing.model.BillReturn;
import com.jwellkeeper.billing.model.BillStatus;
import com.jwellkeeper.billing.model.BillVoid;
import com.jwellkeeper.billing.model.ReturnItem;
import com.jwellkeeper.billing.repository.BillExchangeRepository;
import com.jwellkeeper.billing.repository.BillRepository;
import com.jwellkeeper.billing.repository.BillReturnRepository;
import com.jwellkeeper.billing.repository.BillVoidRepository;
import com.jwellkeeper.billing.repository.ReturnItemRepository;
import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.AuthorizationService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillCorrectionService {

    private final BillRepository billRepository;
    private final BillVoidRepository voidRepository;
    private final BillReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final BillExchangeRepository exchangeRepository;
    private final JewelleryRepository jewelleryRepository;
    private final AuthorizationService authorizationService;
    private final StockMovementService movementService;
    private final BusinessLogService logService;

    @Transactional
    public BillVoidResponse voidBill(UUID billId, BillVoidRequest request) {
        authorizationService.validateOwnerOrManagerPassword(request.password(), "Owner or manager password is required to void a bill");
        UUID tenantId = TenantContext.requireTenantId();
        Bill bill = findBill(billId);
        if (bill.getStatus() == BillStatus.VOIDED) {
            throw new ConflictException("Bill is already voided");
        }
        if (voidRepository.existsByTenantIdAndBillId(tenantId, billId)) {
            throw new ConflictException("Bill void record already exists");
        }
        bill.setStatus(BillStatus.VOIDED);
        bill.getItems().forEach(item -> restoreSoldJewellery(item.getJewelleryId(), bill, StockMovementType.BILL_VOIDED, request.reason()));

        BillVoid billVoid = new BillVoid();
        billVoid.setTenantId(tenantId);
        billVoid.setBillId(bill.getId());
        billVoid.setReason(request.reason().trim());
        billVoid.setCreatedBy(TenantContext.requireUser().userId());
        voidRepository.save(billVoid);
        logService.log("BILL_VOIDED", "Bill", bill.getId(), "SUCCESS", "Bill voided", Map.of("billNo", bill.getBillNo(), "reason", request.reason()));
        return new BillVoidResponse(billVoid.getId(), bill.getId(), bill.getBillNo(), bill.getStatus(), billVoid.getReason(), billVoid.getCreatedBy(), billVoid.getCreatedAt());
    }

    @Transactional
    public BillReturnResponse returnItems(UUID billId, BillReturnRequest request) {
        authorizationService.validateOwnerOrManagerPassword(request.password(), "Owner or manager password is required to return bill items");
        UUID tenantId = TenantContext.requireTenantId();
        Bill bill = findBill(billId);
        if (bill.getStatus() == BillStatus.VOIDED || bill.getStatus() == BillStatus.RETURNED) {
            throw new ConflictException("Bill cannot accept more returns");
        }

        var requested = new HashSet<UUID>();
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (ReturnItemRequest itemRequest : request.items()) {
            if (!requested.add(itemRequest.billItemId())) {
                throw new ConflictException("The same bill item cannot be returned twice in one request");
            }
            BillItem billItem = bill.getItems().stream()
                    .filter(item -> item.getId().equals(itemRequest.billItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Bill item not found: " + itemRequest.billItemId()));
            BigDecimal refundAmount = itemRequest.refundAmount() == null ? billItem.getFinalPrice() : itemRequest.refundAmount();
            refundTotal = refundTotal.add(refundAmount);
        }

        BillReturn billReturn = new BillReturn();
        billReturn.setTenantId(tenantId);
        billReturn.setBillId(bill.getId());
        billReturn.setReason(request.reason().trim());
        billReturn.setRefundAmount(refundTotal);
        billReturn.setCreatedBy(TenantContext.requireUser().userId());
        returnRepository.save(billReturn);

        for (ReturnItemRequest itemRequest : request.items()) {
            BillItem billItem = bill.getItems().stream()
                    .filter(item -> item.getId().equals(itemRequest.billItemId()))
                    .findFirst()
                    .orElseThrow();
            boolean restock = itemRequest.restock() == null || itemRequest.restock();
            ReturnItem returnItem = new ReturnItem();
            returnItem.setTenantId(tenantId);
            returnItem.setReturnId(billReturn.getId());
            returnItem.setBillItemId(billItem.getId());
            returnItem.setJewelleryId(billItem.getJewelleryId());
            returnItem.setRefundAmount(itemRequest.refundAmount() == null ? billItem.getFinalPrice() : itemRequest.refundAmount());
            returnItem.setRestock(restock);
            returnItemRepository.save(returnItem);
            if (restock) {
                restoreSoldJewellery(billItem.getJewelleryId(), bill, StockMovementType.RETURNED, request.reason());
            }
        }

        bill.setStatus(requested.size() == bill.getItems().size() ? BillStatus.RETURNED : BillStatus.PARTIALLY_RETURNED);
        logService.log("BILL_RETURNED", "Bill", bill.getId(), "SUCCESS", "Bill return recorded", Map.of("billNo", bill.getBillNo(), "items", requested.size(), "refundTotal", refundTotal));
        return new BillReturnResponse(billReturn.getId(), bill.getId(), bill.getBillNo(), bill.getStatus(), refundTotal, billReturn.getReason(), billReturn.getCreatedBy(), billReturn.getCreatedAt());
    }

    @Transactional
    public BillExchangeResponse exchange(UUID billId, BillExchangeRequest request) {
        authorizationService.validateOwnerOrManagerPassword(request.password(), "Owner or manager password is required to exchange bill items");
        Bill bill = findBill(billId);
        BillExchange exchange = new BillExchange();
        exchange.setTenantId(TenantContext.requireTenantId());
        exchange.setBillId(bill.getId());
        exchange.setReason(request.reason().trim());
        exchange.setCreatedBy(TenantContext.requireUser().userId());
        exchange.setMetadata("{\"status\":\"SCAFFOLD\"}");
        exchangeRepository.save(exchange);
        logService.log("BILL_EXCHANGE_SCAFFOLD", "Bill", bill.getId(), "SUCCESS", "Bill exchange scaffold recorded", Map.of("billNo", bill.getBillNo(), "reason", request.reason()));
        return new BillExchangeResponse(exchange.getId(), bill.getId(), bill.getBillNo(), exchange.getReason(), exchange.getCreatedBy(), exchange.getCreatedAt(), "Exchange scaffold saved. Full exchange item flow can be expanded from this record.");
    }

    private Bill findBill(UUID billId) {
        return billRepository.findByIdAndTenantId(billId, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    private void restoreSoldJewellery(UUID jewelleryId, Bill bill, StockMovementType movementType, String reason) {
        Jewellery jewellery = jewelleryRepository.lockByIdAndTenantId(jewelleryId, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery not found: " + jewelleryId));
        if (jewellery.getStatus() == JewelleryStatus.SOLD) {
            JewelleryStatus fromStatus = jewellery.getStatus();
            jewellery.setStatus(JewelleryStatus.AVAILABLE);
            jewellery.setSoldAt(null);
            movementService.record(
                    jewellery,
                    movementType,
                    "Bill",
                    bill.getId(),
                    fromStatus,
                    jewellery.getStatus(),
                    reason,
                    Map.of("billNo", bill.getBillNo())
            );
        }
    }
}
