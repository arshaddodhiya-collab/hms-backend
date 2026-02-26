package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.response.PaymentResponse;
import com.hms.HospitalManagementSystem.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "paymentMethod", expression = "java(payment.getPaymentMethod().name())")
    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "receivedBy", source = "receivedBy.id")
    PaymentResponse toResponse(Payment payment);
}
