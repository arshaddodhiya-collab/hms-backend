package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
import com.hms.HospitalManagementSystem.dto.response.RoundResponse;
import com.hms.HospitalManagementSystem.dto.response.VitalsResponse;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.Round;
import com.hms.HospitalManagementSystem.entity.Vitals;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EncounterMapper {

    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "admissionId", source = "admission.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(encounter.getPatient().getFirstName() + \" \" + encounter.getPatient().getLastName())")
    @Mapping(target = "patientGender", expression = "java(encounter.getPatient().getGender().name())")
    @Mapping(target = "patientDob", expression = "java(encounter.getPatient().getDob().toString())")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    @Mapping(target = "status", expression = "java(encounter.getStatus().name())")
    // MapStruct will automatically use toVitalsResponse for the single vitals and
    // list mapping if configured right or we can spell it out
    @Mapping(target = "vitals", expression = "java(encounter.getVitals() != null ? toVitalsResponse(encounter.getVitals()) : null)")
    EncounterResponse toResponse(Encounter encounter);

    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "recordedBy", source = "recordedBy.fullName")
    VitalsResponse toVitalsResponse(Vitals vitals);

    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    RoundResponse toRoundResponse(Round round);
}
