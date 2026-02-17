package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Charge;
import com.hms.HospitalManagementSystem.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByPatientIdAndStatus(Long patientId, ChargeStatus status);

    List<Charge> findByInvoiceId(Long invoiceId);
}
