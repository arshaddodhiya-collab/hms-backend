package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    Optional<Encounter> findByAppointmentId(Long appointmentId);

    List<Encounter> findByStatus(EncounterStatus status);

    List<Encounter> findByDoctorIdAndStatus(Long doctorId, EncounterStatus status);

    @Query("SELECT e FROM Encounter e WHERE e.doctor.id = :doctorId AND e.status IN :statuses")
    List<Encounter> findByDoctorIdAndStatusIn(Long doctorId, List<EncounterStatus> statuses);
}
