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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class PatientControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.hms.HospitalManagementSystem.mapper.EncounterMapper encounterMapper;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.hms.HospitalManagementSystem.mapper.PaymentMapper paymentMapper;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.hms.HospitalManagementSystem.mapper.InvoiceMapper invoiceMapper;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.hms.HospitalManagementSystem.mapper.IpdMapper ipdMapper;

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
                String uniqueSuffix = java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 7);
                PatientRegisterRequest request = PatientRegisterRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .dob(LocalDate.of(1990, 1, 1))
                                .gender(Patient.Gender.MALE)
                                .contact("999" + uniqueSuffix) // Unique 10-digit contact number for test
                                .email("john.doe." + java.util.UUID.randomUUID().toString().substring(0, 8)
                                                + "@example.com") // Unique email for test
                                .build();

                // When/Then
                mockMvc.perform(post("/api/v1/patients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }
}
