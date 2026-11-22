package com.rupeek.hotel.api;

import com.rupeek.hotel.api.ApiDtos.ErrorResponse;
import com.rupeek.hotel.exception.InvalidStateException;
import com.rupeek.hotel.exception.InventoryUnavailableException;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.exception.ValidationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> validation(ValidationException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(InvalidStateException e) {
        return respond(HttpStatus.CONFLICT, "INVALID_STATE", e.getMessage());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<ErrorResponse> unavailable(InventoryUnavailableException e) {
        return respond(HttpStatus.CONFLICT, "INVENTORY_UNAVAILABLE", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> malformedRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed or incomplete request");
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
