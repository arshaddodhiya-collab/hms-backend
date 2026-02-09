package com.hms.HospitalManagementSystem.config;

import com.hms.HospitalManagementSystem.entity.Permission;
import com.hms.HospitalManagementSystem.entity.Role;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.repository.PermissionRepository;
import com.hms.HospitalManagementSystem.repository.RoleRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Seed Permissions
        seedPermissions();

        // 2. Seed Roles
        seedRoles();

        // 3. Seed Users
        seedUsers();
    }

    private void seedPermissions() {
        List<String> permissionCodes = Arrays.asList(
                "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS", "MOD_TRIAGE",
                "MOD_CONSULTATION", "MOD_LAB", "MOD_BILLING", "MOD_ADMIN", "MOD_VOICE",
                "ACT_VIEW", "ACT_CREATE", "ACT_EDIT", "ACT_DELETE");

        for (String code : permissionCodes) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                Permission permission = new Permission();
                permission.setCode(code);
                permission.setModule("GENERAL"); // Simplified for now
                permissionRepository.save(permission);
            }
        }
    }

    private void seedRoles() {
        // ADMIN: All permissions
        List<String> adminPerms = Arrays.asList(
                "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS", "MOD_TRIAGE", "MOD_CONSULTATION",
                "MOD_LAB", "MOD_BILLING", "MOD_ADMIN", "MOD_VOICE",
                "CMP_VITALS_WRITE", "CMP_VITALS_READ",
                "ACT_VIEW", "ACT_CREATE", "ACT_EDIT", "ACT_DELETE",
                "CMP_PATIENT_ADD", "CMP_PATIENT_LIST", "CMP_PATIENT_VIEW", "CMP_PATIENT_EDIT",
                "CMP_APPOINTMENT_LIST", "CMP_APPOINTMENT_CREATE", "CMP_APPOINTMENT_VIEW", "CMP_APPOINTMENT_EDIT",
                "CMP_CONSULTATION_READ", "CMP_CONSULTATION_WRITE",
                "CMP_LAB_ENTRY", "CMP_LAB_READ",
                "CMP_BILLING_SUMMARY", "CMP_INVOICE_GENERATE", "CMP_PAYMENT_RECEIPT",
                "CMP_ADMIN_DEPT_READ", "CMP_ADMIN_DEPT_WRITE",
                "CMP_ADMIN_USER_READ", "CMP_ADMIN_USER_WRITE", "CMP_ADMIN_ROLE_WRITE");
        createOrUpdateRole("ADMIN", "Administrator", adminPerms);

        // DOCTOR
        List<String> doctorPerms = Arrays.asList(
                "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS", "MOD_CONSULTATION",
                "ACT_VIEW", "ACT_CREATE", "ACT_EDIT",
                "CMP_PATIENT_LIST", "CMP_PATIENT_VIEW",
                "CMP_APPOINTMENT_LIST", "CMP_APPOINTMENT_VIEW",
                "CMP_VITALS_READ",
                "CMP_CONSULTATION_WRITE", "CMP_CONSULTATION_READ",
                "CMP_LAB_READ");
        createOrUpdateRole("DOCTOR", "Medical Doctor", doctorPerms);

        // NURSE
        List<String> nursePerms = Arrays.asList(
                "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS", "MOD_TRIAGE", "MOD_CONSULTATION",
                "ACT_VIEW", "ACT_CREATE",
                "CMP_APPOINTMENT_LIST", "CMP_APPOINTMENT_VIEW",
                "CMP_VITALS_WRITE", "CMP_VITALS_READ",
                "CMP_CONSULTATION_READ",
                "CMP_LAB_READ");
        createOrUpdateRole("NURSE", "Nurse", nursePerms);

        // LAB_TECH
        List<String> labPerms = Arrays.asList(
                "MOD_DASHBOARD", "MOD_LAB",
                "ACT_VIEW", "ACT_CREATE",
                "CMP_LAB_ENTRY", "CMP_LAB_READ");
        createOrUpdateRole("LAB_TECH", "Lab Technician", labPerms); // Frontend says "Lab Technician", logic maps to
                                                                    // LAB_TECH

        // RECEPTION
        List<String> receptionPerms = Arrays.asList(
                "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS", "MOD_BILLING",
                "ACT_VIEW", "ACT_CREATE", "ACT_EDIT",
                "CMP_PATIENT_ADD", "CMP_PATIENT_LIST", "CMP_PATIENT_VIEW", "CMP_PATIENT_EDIT",
                "CMP_APPOINTMENT_LIST", "CMP_APPOINTMENT_CREATE", "CMP_APPOINTMENT_VIEW", "CMP_APPOINTMENT_EDIT",
                "CMP_INVOICE_GENERATE", "CMP_PAYMENT_RECEIPT", "CMP_BILLING_SUMMARY");
        createOrUpdateRole("RECEPTION", "Front Desk", receptionPerms);
    }

    private void createOrUpdateRole(String name, String description, List<String> permissionCodes) {
        Role role = roleRepository.findByName(name).orElse(new Role(name));
        role.setDescription(description);

        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissionRepository.findByCode(code).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);

        roleRepository.save(role);
    }

    private void seedUsers() {
        createUserIfNotFound("admin", "admin123", "System Administrator", "ADMIN");
        createUserIfNotFound("doctor", "doctor123", "Dr. House", "DOCTOR");
    }

    private void createUserIfNotFound(String username, String password, String fullName, String roleName) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setActive(true);

            roleRepository.findByName(roleName).ifPresent(role -> {
                Set<Role> roles = new HashSet<>();
                roles.add(role);
                user.setRoles(roles);
            });

            userRepository.save(user);
        }
    }
}
