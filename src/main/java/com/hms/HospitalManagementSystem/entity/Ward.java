package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.WardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WardType type;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bed> beds = new ArrayList<>();
}
