package cl.induztek.ms_usuarios.dto;

public record LoginResponse(

    String token,

    String tipoToken,

    Long idUsuario,

    Long rolId,

    String rolNombre,

    boolean requiereCambioContrasena,

    UsuarioResponse usuario

) {
    @Override
    public String toString() {
        return "LoginResponse";
    }
}