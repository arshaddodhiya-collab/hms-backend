package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.BedType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "beds", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "ward_id", "number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedType type;

    @Column(name = "is_occupied", nullable = false)
    private boolean isOccupied = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;
}
