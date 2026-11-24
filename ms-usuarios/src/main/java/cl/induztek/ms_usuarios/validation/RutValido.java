package cl.induztek.ms_usuarios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RutValidator.class)
@Target({
    ElementType.FIELD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.METHOD,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface RutValido {

    String message() default "El RUT ingresado no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}