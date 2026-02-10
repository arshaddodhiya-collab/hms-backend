package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends PublicEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_of_department_id")
    private User headOfDepartment;

    @Builder.Default
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
}
