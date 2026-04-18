package com.jwellkeeper.audit.mapper;

import com.jwellkeeper.audit.dto.StockAuditItemResponse;
import com.jwellkeeper.audit.model.StockAuditItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockAuditMapper {

    StockAuditItemResponse toItemResponse(StockAuditItem item);
}
