package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.response.InvoiceItemResponse;
import com.hms.HospitalManagementSystem.entity.InvoiceItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {
    InvoiceItemResponse toResponse(InvoiceItem entity);
}
