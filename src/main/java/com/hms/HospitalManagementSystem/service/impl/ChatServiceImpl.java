package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.ChatMessageResponse;
import com.hms.HospitalManagementSystem.dto.response.BillingSummaryResponse;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.repository.ChatMessageRepository;
import com.hms.HospitalManagementSystem.service.*;
import com.hms.HospitalManagementSystem.service.chat.IntentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final IntentParser intentParser;
    private final AppointmentService appointmentService;
    private final LabService labService;
    private final PrescriptionService prescriptionService;
    private final BillingService billingService;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(User patient, String content) {
        // 1. Save User Message
        ChatMessage userMessage = ChatMessage.builder()
                .patient(patient)
                .content(content)
                .sender(SenderType.USER)
                .type(MessageType.TEXT)
                .build();
        chatMessageRepository.save(userMessage);

        // 2. Determine Intent
        IntentParser.Intent intent = intentParser.parseIntent(content);
        String responseContent = "";
        MessageType responseType = MessageType.TEXT;

        // 3. Process Intent
        switch (intent) {
            case APPOINTMENT:
                responseContent = handleAppointmentQuery(patient);
                responseType = MessageType.CARD; // Simplified for now, could be text or card
                break;
            case LAB_RESULT:
                responseContent = handleLabResultQuery(patient);
                break;
            case PRESCRIPTION:
                responseContent = handlePrescriptionQuery(patient);
                break;
            case BILLING:
                responseContent = handleBillingQuery(patient);
                break;
            case UNKNOWN:
            default:
                responseContent = "I'm sorry, I didn't understand that. You can ask me about Appointments, Lab Results, Prescriptions, or Billing.";
                break;
        }

        // 4. Save Bot Response
        ChatMessage botMessage = ChatMessage.builder()
                .patient(patient)
                .content(responseContent)
                .sender(SenderType.BOT)
                .type(responseType)
                .build();
        chatMessageRepository.save(botMessage);

        return mapToResponse(botMessage);
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(User patient) {
        return chatMessageRepository.findByPatientIdOrderByTimestampAsc(patient.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .patientId(message.getPatient().getId())
                .content(message.getContent())
                .sender(message.getSender())
                .type(message.getType())
                .timestamp(message.getTimestamp())
                .build();
    }

    private String handleAppointmentQuery(User patient) {
        List<Appointment> appointments = appointmentService.getPatientAppointmentsByStatus(patient.getId(),
                AppointmentStatus.SCHEDULED);
        if (appointments.isEmpty()) {
            return "You have no upcoming appointments.";
        }
        StringBuilder sb = new StringBuilder("Here are your upcoming appointments:\n");
        for (Appointment appt : appointments) {
            sb.append(String.format("- %s with Dr. %s on %s\n",
                    appt.getType(),
                    appt.getDoctor().getFullName(),
                    appt.getStartDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        return sb.toString();
    }

    private String handleLabResultQuery(User patient) {
        List<LabRequest> labRequests = labService.getPatientLabRequests(patient.getId());
        if (labRequests.isEmpty()) {
            return "You have no lab results on file.";
        }

        // Filter for completed requests (results available)
        List<LabRequest> completedRequests = labRequests.stream()
                .filter(req -> req.getStatus() == com.hms.HospitalManagementSystem.enums.LabRequestStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completedRequests.isEmpty()) {
            return "You have pending lab requests, but no results are ready yet.";
        }

        StringBuilder sb = new StringBuilder("Here are your latest lab results:\n");
        // Limit to 3 most recent
        completedRequests.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .limit(3)
                .forEach(req -> {
                    sb.append(String.format("- %s (Ordered: %s)\n",
                            req.getTestName(),
                            req.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
                });

        return sb.toString();
    }

    private String handlePrescriptionQuery(User patient) {
        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patient.getId());
        if (prescriptions.isEmpty()) {
            return "You have no prescriptions on file.";
        }
        StringBuilder sb = new StringBuilder("Here are your recent prescriptions:\n");
        for (Prescription p : prescriptions) {
            sb.append(String.format("- Prescribed by Dr. %s on %s: %s\n",
                    p.getEncounter().getDoctor().getFullName(),
                    p.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    p.getNote()));
        }
        return sb.toString();
    }

    private String handleBillingQuery(User patient) {
        BillingSummaryResponse summary = billingService.getBillingSummary(patient.getId());
        List<InvoiceResponse> outstanding = billingService.getOutstandingInvoices(patient.getId());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Billing Summary:\nTotal Due: %s\n", summary.getTotalDueAmount()));

        if (!outstanding.isEmpty()) {
            sb.append("Outstanding Invoices:\n");
            for (InvoiceResponse inv : outstanding) {
                sb.append(String.format("- Invoice #%s: %s (Due: %s)\n",
                        inv.getInvoiceNumber(),
                        inv.getStatus(),
                        inv.getDueAmount()));
            }
        } else {
            sb.append("You have no outstanding invoices.");
        }
        return sb.toString();
    }
}
