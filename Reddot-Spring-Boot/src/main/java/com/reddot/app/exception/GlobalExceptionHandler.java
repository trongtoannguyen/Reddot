package com.reddot.app.exception;

import com.reddot.app.dto.ErrorObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({Exception.class, RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorObject> handleGeneralException(Exception ex, WebRequest request) {
        ErrorObject errorObject = new ErrorObject(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({BadRequestException.class, MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorObject> handleMethodArgumentNotValidExceptions(Exception ex, WebRequest request) {
        ErrorObject errorObject;
        switch (ex) {
            case MethodArgumentNotValidException methodArgumentNotValidException -> {
                String errorMessage = Objects.requireNonNull(methodArgumentNotValidException.getBindingResult().getFieldError()).getDefaultMessage();
                errorObject = new ErrorObject(HttpStatus.BAD_REQUEST.value(), errorMessage, request.getDescription(false));
            }
            case MethodArgumentTypeMismatchException e ->
                    errorObject = new ErrorObject(HttpStatus.BAD_REQUEST.value(), "Invalid parameter type", request.getDescription(false));
            case HttpMessageNotReadableException e ->
                    errorObject = new ErrorObject(HttpStatus.BAD_REQUEST.value(), "Invalid request body", request.getDescription(false));
            case null, default -> {
                assert ex != null;
                errorObject = new ErrorObject(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request.getDescription(false));
            }
        }
        return new ResponseEntity<>(errorObject, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorObject> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorObject errorObject = new ErrorObject(HttpStatus.FORBIDDEN.value(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorObject, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorObject> handleResourceNotFoundException(Exception ex, WebRequest request) {
        ErrorObject errorObject = new ErrorObject(HttpStatus.NOT_FOUND.value(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorObject, HttpStatus.NOT_FOUND);
    }
}