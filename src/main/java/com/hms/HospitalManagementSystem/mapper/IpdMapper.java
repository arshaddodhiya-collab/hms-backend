package com.hms.HospitalManagementSystem.mapper;

import com.hms.HospitalManagementSystem.dto.ipd.*;
import com.hms.HospitalManagementSystem.entity.Admission;
import com.hms.HospitalManagementSystem.entity.Bed;
import com.hms.HospitalManagementSystem.entity.Ward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IpdMapper {

    // Ward Mappings
    Ward toWard(WardRequest request);

    WardResponse toWardResponse(Ward ward);

    List<WardResponse> toWardResponseList(List<Ward> wards);

    // Bed Mappings
    @Mapping(target = "ward", ignore = true)
    Bed toBed(BedRequest request);

    @Mapping(target = "ward", source = "ward")
    BedResponse toBedResponse(Bed bed);

    List<BedResponse> toBedResponseList(List<Bed> beds);

    // Admission Mappings
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(admission.getPatient().getFirstName() + \" \" + admission.getPatient().getLastName())")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    @Mapping(target = "bed", source = "bed")
    AdmissionResponse toAdmissionResponse(Admission admission);

    List<AdmissionResponse> toAdmissionResponseList(List<Admission> admissions);
}
