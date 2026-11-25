package cl.induztek.ms_protocolos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EvaluacionPrueba {
    public enum Tipo { AISLACION, TTR, RELE, CONTACTOS, DEVANADOS }
    public enum Resultado { APROBADO, PRECAUCION, RECHAZADO, PENDIENTE, REVISION_MANUAL }

    public record Evaluacion(Resultado resultado, BigDecimal indicePolarizacion) {}

    private EvaluacionPrueba() {}

    public static Evaluacion evaluar(Tipo tipo, BigDecimal referencia, BigDecimal lectura) {
        if (tipo == Tipo.DEVANADOS) {
            return new Evaluacion(Resultado.REVISION_MANUAL, null);
        }
        if (referencia == null || lectura == null || referencia.signum() <= 0 || lectura.signum() < 0) {
            return new Evaluacion(Resultado.PENDIENTE, null);
        }

        return switch (tipo) {
            case AISLACION -> new Evaluacion(
                    lectura.compareTo(referencia.multiply(BigDecimal.TWO)) >= 0 ? Resultado.APROBADO
                            : lectura.compareTo(referencia) >= 0 ? Resultado.PRECAUCION : Resultado.RECHAZADO,
                    lectura.divide(referencia, 8, RoundingMode.HALF_UP));
            case TTR -> new Evaluacion(desviacion(referencia, lectura, "0.5", "1"), null);
            case RELE -> new Evaluacion(desviacion(referencia, lectura, "5", "10"), null);
            case CONTACTOS -> new Evaluacion(
                    porUmbrales(referencia, lectura, lectura.multiply(new BigDecimal("1.2"))), null);
            case DEVANADOS -> throw new IllegalStateException("La prueba requiere revision manual");
        };
    }

    public static boolean variacionSignificativa(BigDecimal referencia, BigDecimal lectura) {
        return referencia != null && lectura != null && referencia.signum() != 0
                && lectura.subtract(referencia).abs().multiply(BigDecimal.valueOf(4)).compareTo(referencia.abs()) > 0;
    }

    private static Resultado desviacion(BigDecimal referencia, BigDecimal lectura, String aceptable, String advertencia) {
        BigDecimal diferencia = lectura.subtract(referencia).abs().movePointRight(2);
        return porUmbrales(diferencia, referencia.multiply(new BigDecimal(aceptable)),
                referencia.multiply(new BigDecimal(advertencia)));
    }

    private static Resultado porUmbrales(BigDecimal valor, BigDecimal aceptable, BigDecimal advertencia) {
        if (valor.compareTo(aceptable) <= 0) {
            return Resultado.APROBADO;
        }
        return valor.compareTo(advertencia) <= 0 ? Resultado.PRECAUCION : Resultado.RECHAZADO;
    }
}
