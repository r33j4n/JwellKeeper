package com.jwellkeeper.billing.controller;

import com.jwellkeeper.billing.dto.BillExchangeRequest;
import com.jwellkeeper.billing.dto.BillExchangeResponse;
import com.jwellkeeper.billing.dto.BillReturnRequest;
import com.jwellkeeper.billing.dto.BillReturnResponse;
import com.jwellkeeper.billing.dto.BillVoidRequest;
import com.jwellkeeper.billing.dto.BillVoidResponse;
import com.jwellkeeper.billing.service.BillCorrectionService;
import com.jwellkeeper.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bills")
public class BillingV2Controller {

    private final BillCorrectionService correctionService;

    @PostMapping("/{billId}/void")
    public ApiResponse<BillVoidResponse> voidBill(@PathVariable UUID billId, @Valid @RequestBody BillVoidRequest request) {
        return ApiResponse.success("Bill voided", correctionService.voidBill(billId, request));
    }

    @PostMapping("/{billId}/returns")
    public ApiResponse<BillReturnResponse> returnItems(@PathVariable UUID billId, @Valid @RequestBody BillReturnRequest request) {
        return ApiResponse.success("Bill return recorded", correctionService.returnItems(billId, request));
    }

    @PostMapping("/{billId}/exchange")
    public ApiResponse<BillExchangeResponse> exchange(@PathVariable UUID billId, @Valid @RequestBody BillExchangeRequest request) {
        return ApiResponse.success("Bill exchange scaffold recorded", correctionService.exchange(billId, request));
    }
}
