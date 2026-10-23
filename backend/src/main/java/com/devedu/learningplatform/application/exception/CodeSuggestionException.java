package com.devedu.learningplatform.application.exception;

public final class CodeSuggestionException extends RuntimeException {
    private final Kind kind;

    public CodeSuggestionException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public CodeSuggestionException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public enum Kind {
        NOT_CONFIGURED,
        UNAVAILABLE
    }
}
