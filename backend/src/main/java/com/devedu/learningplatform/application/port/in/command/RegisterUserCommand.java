package com.devedu.learningplatform.application.port.in.command;

public record RegisterUserCommand(String name, String email, String password) {
}
