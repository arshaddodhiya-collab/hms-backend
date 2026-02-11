package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

        List<Appointment> findByPatientId(Long patientId);

        List<Appointment> findByDoctorIdAndStartDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

        boolean existsByDoctorIdAndStartDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

        List<Appointment> findByStartDateTimeBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
                        "WHERE a.doctor.id = :doctorId " +
                        "AND a.status <> 'CANCELLED' " +
                        "AND a.startDateTime < :endDateTime " +
                        "AND a.endDateTime > :startDateTime")
        boolean existsOverlappingAppointment(@Param("doctorId") Long doctorId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);
}
