package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.response.UserDto;
import com.hms.HospitalManagementSystem.entity.Role;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.repository.DepartmentRepository;
import com.hms.HospitalManagementSystem.repository.RoleRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .noneMatch(r -> r.getName().equalsIgnoreCase("PATIENT")))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return mapToDto(user);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Override
    public List<UserDto> getUsersByRole(String roleName) {
        return userRepository.findByRoles_Name(roleName).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto createUser(UserDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists: " + dto.getUsername());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // Use provided password or default '123'
        String pwd = dto.getPassword() != null && !dto.getPassword().isEmpty() ? dto.getPassword() : "123";
        user.setPassword(passwordEncoder.encode(pwd));
        user.setFullName(dto.getFullName());
        user.setActive(dto.isActive());

        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId())
                    .ifPresent(user::setDepartment);
        }

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : dto.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        return mapToDto(userRepository.save(user));
    }

    @Override
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        user.setFullName(dto.getFullName());
        user.setActive(dto.isActive());

        // Update Department
        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId())
                    .ifPresent(user::setDepartment);
        } else {
            user.setDepartment(null);
        }

        // Update Roles if provided
        if (dto.getRoles() != null) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : dto.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        return mapToDto(userRepository.save(user));
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .active(user.isActive())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}
