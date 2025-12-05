package com.arquitetura.pedidos.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
    MethodArgumentNotValidException ex,
    WebRequest request
  ) {
    log.error("❌ [EXCEPTION] Erro de validação detectado");

    Map<String, String> errors = new HashMap<>();
    ex
      .getBindingResult()
      .getAllErrors()
      .forEach(error -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
        log.error("   └─ Campo '{}': {}", fieldName, errorMessage);
      });

    ErrorResponse errorResponse = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Erro de Validação")
      .message("Os dados fornecidos são inválidos")
      .validationErrors(errors)
      .path(request.getDescription(false).replace("uri=", ""))
      .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(
    Exception ex,
    WebRequest request
  ) {
    log.error("❌ [EXCEPTION] Erro inesperado: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error("Erro Interno do Servidor")
      .message(ex.getMessage())
      .path(request.getDescription(false).replace("uri=", ""))
      .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      errorResponse
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
    IllegalArgumentException ex,
    WebRequest request
  ) {
    log.error("❌ [EXCEPTION] Argumento inválido: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Argumento Inválido")
      .message(ex.getMessage())
      .path(request.getDescription(false).replace("uri=", ""))
      .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }
}
