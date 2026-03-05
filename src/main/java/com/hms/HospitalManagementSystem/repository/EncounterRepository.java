package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    Optional<Encounter> findByAppointmentId(Long appointmentId);

    List<Encounter> findByStatus(EncounterStatus status);

    Slice<Encounter> findByStatus(EncounterStatus status,
                                  org.springframework.data.domain.Pageable pageable);

    List<Encounter> findByPatientId(Long patientId);

    Slice<Encounter> findByPatientId(Long patientId,
            org.springframework.data.domain.Pageable pageable);

    List<Encounter> findByDoctorIdAndStatus(Long doctorId, EncounterStatus status);

    @Query("SELECT e FROM Encounter e WHERE e.doctor.id = :doctorId AND e.status IN :statuses")
    List<Encounter> findByDoctorIdAndStatusIn(Long doctorId, List<EncounterStatus> statuses);

    @Query("SELECT e FROM Encounter e WHERE e.doctor.id = :doctorId AND e.status IN :statuses")
    Slice<Encounter> findByDoctorIdAndStatusIn(Long doctorId,
            List<EncounterStatus> statuses, org.springframework.data.domain.Pageable pageable);

    List<Encounter> findByDoctorIdAndAppointmentIsNotNullAndStatusIn(Long doctorId, List<EncounterStatus> statuses);

    Slice<Encounter> findByDoctorIdAndAppointmentIsNotNullAndStatusIn(Long doctorId,
            List<EncounterStatus> statuses, org.springframework.data.domain.Pageable pageable);

    List<Encounter> findByDoctorIdAndAdmissionIsNotNullAndStatusIn(Long doctorId, List<EncounterStatus> statuses);

    Slice<Encounter> findByDoctorIdAndAdmissionIsNotNullAndStatusIn(Long doctorId,
            List<EncounterStatus> statuses, org.springframework.data.domain.Pageable pageable);

    Optional<Encounter> findByAdmissionId(Long admissionId);
}
