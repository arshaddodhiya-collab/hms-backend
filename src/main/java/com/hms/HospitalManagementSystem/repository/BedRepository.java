package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Bed;
import com.hms.HospitalManagementSystem.enums.BedType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {

    @Query("SELECT b FROM Bed b WHERE b.ward.id = :wardId AND b.isOccupied = false AND b.isActive = true")
    Slice<Bed> findAvailableBedsByWard(@Param("wardId") Long wardId,
                                       org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM Bed b WHERE b.ward.id = :wardId AND b.type = :type AND b.isOccupied = false AND b.isActive = true")
    Slice<Bed> findAvailableBedsByWardAndType(@Param("wardId") Long wardId,
            @Param("type") BedType type, org.springframework.data.domain.Pageable pageable);

    Slice<Bed> findAllBy(org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b WHERE b.id = :id")
    Optional<Bed> findByIdWithLock(@Param("id") Long id);
}
