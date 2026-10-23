package com.devedu.learningplatform.application.exception;

public final class EmailVerificationException extends RuntimeException {

    public enum Kind {
        INVALID_OR_EXPIRED,
        TOO_MANY_ATTEMPTS,
        RESEND_TOO_SOON
    }

    private final Kind kind;
    private final long retryAfterSeconds;

    private EmailVerificationException(Kind kind, String message, long retryAfterSeconds) {
        super(message);
        this.kind = kind;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static EmailVerificationException invalidOrExpired() {
        return new EmailVerificationException(
                Kind.INVALID_OR_EXPIRED,
                "Mã xác nhận không đúng hoặc đã hết hạn",
                0
        );
    }

    public static EmailVerificationException tooManyAttempts() {
        return new EmailVerificationException(
                Kind.TOO_MANY_ATTEMPTS,
                "Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới",
                0
        );
    }

    public static EmailVerificationException resendTooSoon(long retryAfterSeconds) {
        return new EmailVerificationException(
                Kind.RESEND_TOO_SOON,
                "Vui lòng đợi " + retryAfterSeconds + " giây trước khi gửi lại mã",
                retryAfterSeconds
        );
    }

    public Kind kind() {
        return kind;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
