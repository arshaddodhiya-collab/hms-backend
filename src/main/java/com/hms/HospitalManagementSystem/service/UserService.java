package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.UserDto;
import com.hms.HospitalManagementSystem.entity.User;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();

    UserDto getUserById(Long id);

    User getUserByUsername(String username);

    List<UserDto> getUsersByRole(String roleName);

    UserDto createUser(UserDto dto);

    UserDto updateUser(Long id, UserDto dto);
}
