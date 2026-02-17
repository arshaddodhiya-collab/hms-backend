package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Optional: Optimistic Locking
    // @Version
    // private Long version;

    // I will include version since I used it in Charge.java explicitly as a field?
    // Wait, in Charge.java I wrote:
    // @Version
    // private Long version;
    // So if Charge has it, BaseEntity doesn't need to enforce it, or Charge
    // overrides it?
    // In Charge.java I defined `private Long version;` explicitly.
    // So BaseEntity strictly needs ID, createdAt, updatedAt.
}
