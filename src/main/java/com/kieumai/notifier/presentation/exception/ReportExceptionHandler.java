package com.kieumai.notifier.presentation.exception;

import com.kieumai.notifier.business.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestControllerAdvice
public class ReportExceptionHandler {
    public record FieldError(String field, String message) {}
    public record ApiError(OffsetDateTime timestamp, int status, String code, String message,
                           String path, List<FieldError> fieldErrors) {}

    @ExceptionHandler(ReportException.class)
    public ResponseEntity<ApiError> handleReport(ReportException exception, HttpServletRequest request) {
        int status = switch (exception.getCode()) {
            case VALIDATION_ERROR -> 400;
            case UNAUTHORIZED -> 401;
            case REPORT_NOT_OWNED -> 403;
            case INCIDENT_NOT_FOUND, REPORT_NOT_FOUND, WARD_NOT_FOUND -> 404;
        };
        return error(status, exception.getCode().name(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldError(field.getField(), field.getDefaultMessage())).toList();
        return error(400, "VALIDATION_ERROR", "Invalid request data.", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return error(400, "VALIDATION_ERROR", "Malformed JSON or request parameter.", request, List.of());
    }

    @ExceptionHandler({WardNotFoundException.class, SubscriptionNotFoundException.class})
    public ResponseEntity<ApiError> handleMissingReference(RuntimeException exception, HttpServletRequest request) {
        return error(404, "NOT_FOUND", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    public ResponseEntity<ApiError> handleDuplicateSubscription(AlreadySubscribedException exception,
                                                               HttpServletRequest request) {
        return error(409, "ALREADY_SUBSCRIBED", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(409, "DATA_CONFLICT", "The request conflicts with stored data.", request, List.of());
    }

    private ResponseEntity<ApiError> error(int status, String code, String message,
                                           HttpServletRequest request, List<FieldError> fields) {
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status,
                code, message, request.getRequestURI(), fields));
    }
}
