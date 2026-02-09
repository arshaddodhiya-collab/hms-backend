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
        createRoleIfNotFound("ADMIN", "Administrator", "MOD_ADMIN", "ACT_VIEW", "ACT_CREATE", "ACT_EDIT", "ACT_DELETE");
        createRoleIfNotFound("DOCTOR", "Medical Doctor", "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_CONSULTATION",
                "ACT_VIEW", "ACT_CREATE", "ACT_EDIT");
        createRoleIfNotFound("NURSE", "Nurse", "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_TRIAGE", "ACT_VIEW", "ACT_CREATE");
        createRoleIfNotFound("RECEPTION", "Receptionist", "MOD_DASHBOARD", "MOD_PATIENTS", "MOD_APPOINTMENTS",
                "ACT_VIEW", "ACT_CREATE");
    }

    private void createRoleIfNotFound(String name, String description, String... permissionCodes) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);

            Set<Permission> permissions = new HashSet<>();
            for (String code : permissionCodes) {
                permissionRepository.findByCode(code).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);

            roleRepository.save(role);
        }
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
