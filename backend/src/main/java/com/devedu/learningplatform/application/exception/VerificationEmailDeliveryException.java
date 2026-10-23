package com.devedu.learningplatform.application.exception;

public final class VerificationEmailDeliveryException extends RuntimeException {

    public VerificationEmailDeliveryException(String message) {
        super(message);
    }

    public VerificationEmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
