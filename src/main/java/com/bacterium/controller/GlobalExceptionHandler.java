package com.bacterium.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Errores de validación (@NotBlank, @NotNull, etc.) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos invalidos", errores);
    }

    /** Credenciales incorrectas en login */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", "Usuario o contraseña invalidos");
    }

    /** Usuario deshabilitado */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Cuenta deshabilitada", "El usuario esta inactivo");
    }

    /** Acceso denegado (403) — no debe llegar aquí si SecurityConfig está bien,
     *  pero por si acaso lo capturamos igualmente */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permiso para realizar esta accion");
    }

    /** Entidad no encontrada (RuntimeException genérico del código) */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.warn("RuntimeException: {}", ex.getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, "Error en la operacion", ex.getMessage());
    }

    /** Cualquier otro error inesperado */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error inesperado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "Ocurrio un error inesperado. Consulte los logs.");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
