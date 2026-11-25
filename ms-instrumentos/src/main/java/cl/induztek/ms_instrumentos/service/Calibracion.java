package cl.induztek.ms_instrumentos.service;

import java.time.LocalDate;

public enum Calibracion {
    VENCIDA, POR_VENCER, VIGENTE;

    public static Calibracion evaluar(LocalDate vencimiento, LocalDate hoy) {
        if (vencimiento.isBefore(hoy)) {
            return VENCIDA;
        }
        return vencimiento.isAfter(hoy.plusDays(30)) ? VIGENTE : POR_VENCER;
    }
}
