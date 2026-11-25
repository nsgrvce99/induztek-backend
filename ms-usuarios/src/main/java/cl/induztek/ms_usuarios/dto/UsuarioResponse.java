package cl.induztek.ms_usuarios.dto;

import cl.induztek.ms_usuarios.model.Usuario;

public record UsuarioResponse(

    Long id,
    String rut,
    String nombre,
    String correo,
    Long rolId,
    String rolNombre,
    boolean activo,
    String giro,
    String telefono,
    String cargo,
    String fotoUri,
    boolean archivado

) {

    public static UsuarioResponse desdeEntidad(Usuario u) {

        return new UsuarioResponse(
            u.getId(),
            u.getRut(),
            u.getNombre(),
            u.getCorreo(),
            u.getRol().getId(),
            u.getRol().getNombre(),
            u.isActivo(),
            u.getGiro(),
            u.getTelefono(),
            u.getContactoCargo(),
            u.getFotoUri(),
            u.isArchivado()
        );
    }
}