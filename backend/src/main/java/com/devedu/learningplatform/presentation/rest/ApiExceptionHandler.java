package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.EmailVerificationException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.exception.ProgrammingProblemSlugAlreadyExistsException;
import com.devedu.learningplatform.application.exception.JudgeUnavailableException;
import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.exception.UserNotFoundException;
import com.devedu.learningplatform.application.exception.UserDeletionConflictException;
import com.devedu.learningplatform.application.exception.VerificationEmailDeliveryException;
import com.devedu.learningplatform.application.exception.ProblemGenerationException;
import com.devedu.learningplatform.application.exception.CodeSuggestionException;
import com.devedu.learningplatform.application.exception.LearningDocumentEditingForbiddenException;
import com.devedu.learningplatform.presentation.rest.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailVerificationException.class)
    ResponseEntity<ApiErrorResponse> handleEmailVerification(
            EmailVerificationException exception,
            HttpServletRequest request
    ) {
        var status = exception.kind() == EmailVerificationException.Kind.INVALID_OR_EXPIRED
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.TOO_MANY_REQUESTS;
        var response = ResponseEntity.status(status).cacheControl(CacheControl.noStore());
        if (exception.retryAfterSeconds() > 0) {
            response.header("Retry-After", Long.toString(exception.retryAfterSeconds()));
        }
        return response.body(errorBody(status, exception.getMessage(), request));
    }

    @ExceptionHandler(VerificationEmailDeliveryException.class)
    ResponseEntity<ApiErrorResponse> handleVerificationEmailDelivery(
            VerificationEmailDeliveryException exception,
            HttpServletRequest request
    ) {
        var status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(errorBody(status, exception.getMessage(), request));
    }

    @ExceptionHandler(com.devedu.learningplatform.application.contest.ContestException.class)
    ResponseEntity<ApiErrorResponse> handleContest(com.devedu.learningplatform.application.contest.ContestException exception, HttpServletRequest request) {
        var status = switch (exception.kind()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return error(status, exception.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(ProgrammingProblemNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleProblemNotFound(
            ProgrammingProblemNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ProgrammingProblemSlugAlreadyExistsException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateProblemSlug(
            ProgrammingProblemSlugAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(JudgeUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleJudgeUnavailable(JudgeUnavailableException exception,
                                                             HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(ProblemGenerationException.class)
    ResponseEntity<ApiErrorResponse> handleProblemGeneration(
            ProblemGenerationException exception,
            HttpServletRequest request
    ) {
        var status = exception.kind() == ProblemGenerationException.Kind.INVALID_RESPONSE
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(errorBody(status, exception.getMessage(), request));
    }

    @ExceptionHandler(CodeSuggestionException.class)
    ResponseEntity<ApiErrorResponse> handleCodeSuggestion(
            CodeSuggestionException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException exception,
                                                         HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(UserManagementForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleUserManagementForbidden(UserManagementForbiddenException exception,
                                                                    HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(LearningDocumentEditingForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleLearningDocumentEditingForbidden(
            LearningDocumentEditingForbiddenException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(UserDeletionConflictException.class)
    ResponseEntity<ApiErrorResponse> handleUserDeletionConflict(UserDeletionConflictException exception,
                                                                 HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        var message = exception instanceof HttpMessageNotReadableException
                ? "Request body is invalid"
                : exception instanceof MethodArgumentTypeMismatchException
                ? "Request parameter is invalid"
                : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(errorBody(status, message, request));
    }

    private ApiErrorResponse errorBody(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
    }
}
