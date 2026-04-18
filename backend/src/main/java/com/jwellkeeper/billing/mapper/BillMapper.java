package com.jwellkeeper.billing.mapper;

import com.jwellkeeper.billing.dto.BillItemResponse;
import com.jwellkeeper.billing.dto.BillResponse;
import com.jwellkeeper.billing.model.Bill;
import com.jwellkeeper.billing.model.BillItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillMapper {

    @Mapping(target = "pdfUrl", expression = "java(\"/api/bill/\" + bill.getId() + \"/pdf\")")
    BillResponse toResponse(Bill bill);

    BillItemResponse toItemResponse(BillItem item);
}
