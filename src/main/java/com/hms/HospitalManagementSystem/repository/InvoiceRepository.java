package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Invoice;
import com.hms.HospitalManagementSystem.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByPatientId(Long patientId);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByIssueDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Invoice> findByPatientIdAndStatusIn(Long patientId, List<InvoiceStatus> statuses);

    org.springframework.data.domain.Slice<Invoice> findByPatientIdAndStatusIn(Long patientId,
            List<InvoiceStatus> statuses, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT SUM(i.netAmount) FROM Invoice i WHERE i.issueDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumNetAmountByIssueDateBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
