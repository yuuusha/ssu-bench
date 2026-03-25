package com.diev.api.error;

import com.diev.exception.AppException;
import com.diev.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleTimeout(HttpServletRequest request) {
        return build(ErrorCode.REQUEST_TIMEOUT, null, request, null);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        return build(ErrorCode.VALIDATION_ERROR, null, request, details);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldViolation> details = ex.getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        return build(ErrorCode.VALIDATION_ERROR, null, request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> details = ex.getConstraintViolations()
                .stream()
                .map(v -> new ApiErrorResponse.FieldViolation(
                        v.getPropertyPath().toString(),
                        safeMessage(v.getMessage(), "Invalid value.")
                ))
                .toList();

        return build(ErrorCode.VALIDATION_ERROR, null, request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid value";

        return build(
                ErrorCode.INVALID_PARAMETER,
                "Parameter '" + ex.getName() + "' must be a " + required + ".",
                request,
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.MISSING_PARAMETER,
                "Missing required parameter '" + ex.getParameterName() + "'.",
                request,
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.ACCESS_DENIED,
                safeMessage(ex.getMessage(), "Access denied."),
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.UNAUTHORIZED,
                safeMessage(ex.getMessage(), "Authentication required."),
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, null, request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> details
    ) {
        String finalMessage = safeMessage(message, errorCode.getDefaultMessage());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                errorCode.getStatus().value(),
                errorCode.getStatus().getReasonPhrase(),
                errorCode.getCode(),
                finalMessage,
                request.getRequestURI(),
                requestId(),
                details
        );
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    private ApiErrorResponse.FieldViolation mapFieldError(FieldError fieldError) {
        return new ApiErrorResponse.FieldViolation(
                fieldError.getField(),
                safeMessage(fieldError.getDefaultMessage(), "Invalid value.")
        );
    }

    private String requestId() {
        String requestId = MDC.get("request_id");
        if (requestId == null) {
            requestId = MDC.get("requestId");
        }
        return requestId;
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}