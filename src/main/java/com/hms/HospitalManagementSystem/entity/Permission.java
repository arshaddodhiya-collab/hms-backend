package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "permissions")
public class Permission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // e.g., MOD_PATIENTS

    @Column(nullable = false, length = 50)
    private String module;

    public Permission() {
    }

    public Permission(String code, String module) {
        this.code = code;
        this.module = module;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
