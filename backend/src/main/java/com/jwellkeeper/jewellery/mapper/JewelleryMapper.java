package com.jwellkeeper.jewellery.mapper;

import com.jwellkeeper.jewellery.dto.JewelleryResponse;
import com.jwellkeeper.jewellery.dto.JewelleryTypeResponse;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JewelleryMapper {

    JewelleryTypeResponse toTypeResponse(JewelleryType type);

    @Mapping(source = "type.name", target = "typeName")
    @Mapping(target = "billNo", ignore = true)
    JewelleryResponse toResponse(Jewellery jewellery);
}
