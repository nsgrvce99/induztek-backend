package cl.induztek.ms_usuarios.dto;

public record PersonalCreadoResponse(

    UsuarioResponse usuario,
    String contrasenaTemporal

) {}