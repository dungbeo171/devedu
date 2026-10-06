package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.exception.ProgrammingProblemSlugAlreadyExistsException;
import com.devedu.learningplatform.application.exception.JudgeUnavailableException;
import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.exception.UserNotFoundException;
import com.devedu.learningplatform.application.exception.UserDeletionConflictException;
import com.devedu.learningplatform.presentation.rest.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

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
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        ));
    }
}
