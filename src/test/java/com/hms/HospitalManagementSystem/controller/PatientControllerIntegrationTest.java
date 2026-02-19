package com.hms.HospitalManagementSystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.HospitalManagementSystem.dto.request.PatientRegisterRequest;
import com.hms.HospitalManagementSystem.entity.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldReturnBadRequestWhenRegistrationDetailsAreInvalid() throws Exception {
        // Given a request with missing required fields (firstName, lastName, etc.)
        PatientRegisterRequest request = PatientRegisterRequest.builder()
                .lastName("Doe") // Missing firstName
                .dob(LocalDate.now().plusDays(1)) // Future date
                .gender(Patient.Gender.MALE)
                .contact("123") // Invalid contact
                .email("invalid-email") // Invalid email
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnCreatedWhenRegistrationDetailsAreValid() throws Exception {
        // Given
        PatientRegisterRequest request = PatientRegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(1990, 1, 1))
                .gender(Patient.Gender.MALE)
                .contact("9999999999") // Unique contact number for test
                .email("unique.john.doe@example.com") // Unique email for test
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
