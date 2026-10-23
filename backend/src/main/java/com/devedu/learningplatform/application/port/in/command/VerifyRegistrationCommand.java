package com.devedu.learningplatform.application.port.in.command;

public record VerifyRegistrationCommand(String email, String code) {
}
