package com.hms.HospitalManagementSystem.service.chat;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class IntentParser {

    private static final Pattern APPOINTMENT_PATTERN = Pattern.compile(".*(appoint|visit|schedule|doc).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LAB_PATTERN = Pattern.compile(".*(lab|result|blood|report).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRECEPTION_PATTERN = Pattern.compile(".*(pill|med|drug|presc).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BILLING_PATTERN = Pattern.compile(".*(bill|invoice|payment).*",
            Pattern.CASE_INSENSITIVE);

    public enum Intent {
        APPOINTMENT,
        LAB_RESULT,
        PRESCRIPTION,
        BILLING,
        UNKNOWN
    }

    public Intent parseIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Intent.UNKNOWN;
        }

        if (APPOINTMENT_PATTERN.matcher(message).matches()) {
            return Intent.APPOINTMENT;
        } else if (LAB_PATTERN.matcher(message).matches()) {
            return Intent.LAB_RESULT;
        } else if (PRECEPTION_PATTERN.matcher(message).matches()) {
            return Intent.PRESCRIPTION;
        } else if (BILLING_PATTERN.matcher(message).matches()) {
            return Intent.BILLING;
        }

        return Intent.UNKNOWN;
    }
}
