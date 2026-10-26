package com.kieumai.notifier.presentation.error;

import com.kieumai.notifier.business.exception.AlreadySubscribedException;
import com.kieumai.notifier.business.exception.InvalidLocationException;
import com.kieumai.notifier.business.exception.NotificationNotFoundException;
import com.kieumai.notifier.business.exception.SubscriptionNotFoundException;
import com.kieumai.notifier.business.exception.WardNotFoundException;
import com.kieumai.notifier.presentation.auth.UnauthenticatedException;
import com.kieumai.notifier.presentation.controller.NearbyAlertController;
import com.kieumai.notifier.presentation.controller.NotificationController;
import com.kieumai.notifier.presentation.controller.SubscriptionController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice(assignableTypes = {
        SubscriptionController.class,
        NotificationController.class,
        NearbyAlertController.class
})
public class NotifierExceptionHandler {

    @ExceptionHandler(WardNotFoundException.class)
    public ResponseEntity<ApiError> handleWardNotFound(WardNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "WARD_NOT_FOUND", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    public ResponseEntity<ApiError> handleAlreadySubscribed(AlreadySubscribedException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "ALREADY_SUBSCRIBED", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ApiError> handleSubscriptionNotFound(SubscriptionNotFoundException e,
                                                               HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFound(NotificationNotFoundException e,
                                                               HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidLocationException.class)
    public ResponseEntity<ApiError> handleInvalidLocation(InvalidLocationException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(UnauthenticatedException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiError.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(field -> new ApiError.FieldError(field.getField(), field.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request data.", request, fields);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(Exception e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Malformed JSON or request parameter.",
                request, List.of());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                                  HttpServletRequest request, List<ApiError.FieldError> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
