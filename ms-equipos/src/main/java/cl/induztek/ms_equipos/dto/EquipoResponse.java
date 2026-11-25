package cl.induztek.ms_equipos.dto;

import cl.induztek.ms_equipos.model.Equipo;

public record EquipoResponse(Long id, String tag, String tipoEquipo, String clasificacion,
                             String instalacion, String marca, String serie, String potenciaTension,
                             Long idCliente, boolean archivado) {
    public static EquipoResponse desde(Equipo equipo) {
        return new EquipoResponse(equipo.getId(), equipo.getTag(), equipo.getTipoEquipo(), "Primario",
                equipo.getInstalacion().getNombre(), equipo.getMarca(), equipo.getSerie(), "",
                equipo.getIdCliente(), equipo.isArchivado());
    }
}
