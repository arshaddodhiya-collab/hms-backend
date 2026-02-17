package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.response.InvoiceItemResponse;
import com.hms.HospitalManagementSystem.entity.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {
    @Mapping(source = "itemDescription", target = "description")
    InvoiceItemResponse toResponse(InvoiceItem entity);
}
