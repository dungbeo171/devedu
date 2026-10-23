package com.devedu.learningplatform.application.port.out;

import java.time.Duration;

public interface VerificationEmailSender {

    void sendRegistrationCode(String recipientEmail, String recipientName, String code, Duration validFor);

    boolean isConfigured();
}
