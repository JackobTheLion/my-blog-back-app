package ru.practicum.yakovlev.api.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.exception.NotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException exception) {
        log.warn("Requested resource was not found: {}", exception.getMessage());
        return createResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Invalid request: {}", exception.getMessage());
        return createResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<ProblemDetail> handleImageStorageException(ImageStorageException exception) {
        log.error("Image storage operation failed: {}", exception.getMessage(), exception);
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> createResponse(HttpStatus status, String message) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problemDetail);
    }

}
