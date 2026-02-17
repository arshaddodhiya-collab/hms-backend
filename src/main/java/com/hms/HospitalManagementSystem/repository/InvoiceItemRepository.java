package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
}
