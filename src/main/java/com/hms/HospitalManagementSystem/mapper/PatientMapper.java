package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.MedicalHistoryDto;
import com.hms.HospitalManagementSystem.dto.request.PatientRegisterRequest;
import com.hms.HospitalManagementSystem.dto.request.PatientUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.PatientDetailsResponse;
import com.hms.HospitalManagementSystem.dto.response.PatientResponse;
import com.hms.HospitalManagementSystem.entity.MedicalHistory;
import com.hms.HospitalManagementSystem.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "medicalHistory", ignore = true)
    @Mapping(target = "version", ignore = true)
    Patient toEntity(PatientRegisterRequest request);

    @Mapping(target = "age", expression = "java(calculateAge(patient.getDob()))")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "age", expression = "java(calculateAge(patient.getDob()))")
    PatientDetailsResponse toDetailsResponse(Patient patient);

    @Mapping(source = "conditionName", target = "condition")
    MedicalHistoryDto toMedicalHistoryDto(MedicalHistory medicalHistory);

    @Mapping(target = "id", ignore = true) // ID from path variable usually
    @Mapping(target = "medicalHistory", ignore = true)
    void updateEntityFromRequest(PatientUpdateRequest request, @MappingTarget Patient patient);

    default Integer calculateAge(LocalDate dob) {
        if (dob == null) {
            return null;
        }
        return Period.between(dob, LocalDate.now()).getYears();
    }
}
