package cl.induztek.ms_usuarios.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
        MethodArgumentNotValidException ex
    ) {

        Map<String, String> errores =
            new LinkedHashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errores.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        Map<String, Object> respuesta =
            new LinkedHashMap<>();

        respuesta.put("status", 400);
        respuesta.put("mensaje", "Error de validación");
        respuesta.put("errores", errores);

        return ResponseEntity.badRequest()
                .body(respuesta);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarNegocio(
        ResponseStatusException ex
    ) {

        Map<String, Object> respuesta =
            new LinkedHashMap<>();

        respuesta.put(
            "status",
            ex.getStatusCode().value()
        );

        respuesta.put(
            "mensaje",
            ex.getReason()
        );

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(respuesta);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(
        DataIntegrityViolationException ex
    ) {

        Map<String, Object> respuesta =
            new LinkedHashMap<>();

        respuesta.put("status", 409);

        respuesta.put(
            "mensaje",
            "El registro está duplicado o incumple "
                + "una restricción de la base de datos"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }
}