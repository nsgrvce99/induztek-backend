package cl.induztek.ms_instrumentos.dto;

import cl.induztek.ms_instrumentos.service.Calibracion;

public record InstrumentoResponse(Long id, String nombre, String marca, String serie, String fechaVencimiento,
                                  Calibracion calibracion) {}