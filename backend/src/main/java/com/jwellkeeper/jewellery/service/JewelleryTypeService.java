package com.jwellkeeper.jewellery.service;

import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.jewellery.dto.CreateJewelleryTypeRequest;
import com.jwellkeeper.jewellery.dto.JewelleryTypeResponse;
import com.jwellkeeper.jewellery.mapper.JewelleryMapper;
import com.jwellkeeper.jewellery.model.JewelleryType;
import com.jwellkeeper.jewellery.repository.JewelleryTypeRepository;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JewelleryTypeService {

    private static final List<String> DEFAULT_TYPES = List.of("Bangle", "Necklace", "Ring", "Earring", "Bracelet", "Pendant", "Chain");

    private final JewelleryTypeRepository repository;
    private final JewelleryMapper mapper;
    private final BusinessLogService logService;

    @Transactional
    public void createDefaults(UUID tenantId) {
        DEFAULT_TYPES.forEach(name -> {
            JewelleryType type = new JewelleryType();
            type.setTenantId(tenantId);
            type.setName(name);
            type.setCustom(false);
            repository.save(type);
        });
    }

    @Transactional
    public JewelleryTypeResponse create(CreateJewelleryTypeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String name = request.name().trim();
        if (repository.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new ConflictException("Jewellery type already exists");
        }
        JewelleryType type = new JewelleryType();
        type.setTenantId(tenantId);
        type.setName(name);
        type.setCustom(true);
        repository.save(type);
        logService.log("JEWELLERY_TYPE_CREATED", "JewelleryType", type.getId(), "SUCCESS", "Jewellery type created", Map.of("name", name));
        return mapper.toTypeResponse(type);
    }

    @Transactional(readOnly = true)
    public List<JewelleryTypeResponse> list() {
        return repository.findByTenantIdOrderByNameAsc(TenantContext.requireTenantId())
                .stream()
                .map(mapper::toTypeResponse)
                .toList();
    }
}
