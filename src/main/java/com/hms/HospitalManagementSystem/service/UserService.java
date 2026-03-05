package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.UserDto;
import com.hms.HospitalManagementSystem.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface UserService {
    Slice<UserDto> getAllUsers(org.springframework.data.domain.Pageable pageable);

    UserDto getUserById(Long id);

    User getUserByUsername(String username);

    org.springframework.data.domain.Slice<UserDto> getUsersByRole(String roleName,
            org.springframework.data.domain.Pageable pageable);

    UserDto createUser(UserDto dto);

    UserDto updateUser(Long id, UserDto dto);
}
