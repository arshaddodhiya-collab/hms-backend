package com.hms.HospitalManagementSystem.mapper;

// import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { InvoiceItemMapper.class })
public interface InvoiceMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(entity.getPatient().getFirstName() + \" \" + entity.getPatient().getLastName())")
    @Mapping(target = "admissionId", source = "admission.id")
    InvoiceResponse toResponse(Invoice entity);
}
