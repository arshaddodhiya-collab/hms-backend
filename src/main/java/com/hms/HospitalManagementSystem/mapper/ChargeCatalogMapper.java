package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.request.ChargeCatalogRequest;
import com.hms.HospitalManagementSystem.dto.response.ChargeCatalogResponse;
import com.hms.HospitalManagementSystem.entity.ChargeCatalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChargeCatalogMapper {

    ChargeCatalog toEntity(ChargeCatalogRequest request);

    ChargeCatalogResponse toResponse(ChargeCatalog entity);
}
