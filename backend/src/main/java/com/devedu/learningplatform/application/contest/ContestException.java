package com.devedu.learningplatform.application.contest;

public class ContestException extends RuntimeException {
    public enum Kind { NOT_FOUND, FORBIDDEN, CONFLICT }
    private final Kind kind;
    public ContestException(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind kind() { return kind; }
}
