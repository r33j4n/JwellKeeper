package com.jwellkeeper.stock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.stock.model.StockMovement;
import com.jwellkeeper.stock.model.StockMovementType;
import com.jwellkeeper.stock.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository repository;
    private final ObjectMapper objectMapper;

    public void record(
            Jewellery jewellery,
            StockMovementType type,
            String sourceType,
            UUID sourceId,
            JewelleryStatus fromStatus,
            JewelleryStatus toStatus,
            String reason,
            Map<String, ?> metadata
    ) {
        var user = TenantContext.current().orElse(null);
        StockMovement movement = new StockMovement();
        movement.setTenantId(jewellery.getTenantId());
        movement.setJewelleryId(jewellery.getId());
        movement.setMovementType(type);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setFromStatus(fromStatus);
        movement.setToStatus(toStatus);
        movement.setWeight(jewellery.getWeight() == null ? BigDecimal.ZERO : jewellery.getWeight());
        movement.setActorUserId(user == null ? null : user.userId());
        movement.setReason(reason);
        movement.setMetadata(writeMetadata(metadata));
        repository.save(movement);
    }

    private String writeMetadata(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
    }
}
