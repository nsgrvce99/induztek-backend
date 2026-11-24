package cl.induztek.ms_usuarios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class RutValidator
        implements ConstraintValidator<RutValido, String> {

    @Override
    public boolean isValid(
            String rut,
            ConstraintValidatorContext context
    ) {

        if (rut == null) {
            return false;
        }

        String limpio = normalizar(rut);

        if (!limpio.matches("[0-9]{1,8}[0-9K]")) {
            return false;
        }

        String cuerpo = limpio.substring(
            0, limpio.length() - 1
        );

        char dvIngresado =
            limpio.charAt(limpio.length() - 1);

        int suma = 0;
        int multiplicador = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {

            int numero = Character.digit(
                cuerpo.charAt(i), 10
            );

            suma += numero * multiplicador;

            multiplicador =
                multiplicador == 7
                    ? 2
                    : multiplicador + 1;
        }

        int resultado = 11 - (suma % 11);

        char dvEsperado;

        if (resultado == 11) {
            dvEsperado = '0';
        } else if (resultado == 10) {
            dvEsperado = 'K';
        } else {
            dvEsperado = Character.forDigit(resultado, 10);
        }

        return dvIngresado == dvEsperado;
    }

    public static String normalizar(String rut) {

        String limpio = rut
                .replace(".", "")
                .replace("-", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (limpio.length() < 2) {
            return limpio;
        }

        // Evita duplicados por ceros iniciales.
        String cuerpo = limpio
                .substring(0, limpio.length() - 1)
                .replaceFirst("^0+(?!$)", "");

        return cuerpo + limpio.charAt(limpio.length() - 1);
    }
}