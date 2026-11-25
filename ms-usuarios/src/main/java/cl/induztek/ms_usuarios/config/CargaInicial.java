package cl.induztek.ms_usuarios.config;

import cl.induztek.ms_usuarios.security.CredencialesService;
import cl.induztek.ms_usuarios.validation.RutValidator;

import org.springframework.boot.ApplicationRunner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.core.env.Environment;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class CargaInicial {

    private record UsuarioInicial(

        Long id,

        Long rolId,

        String rut,

        String nombre,

        String correo

    ) {}

    @Bean
    @Profile("seed")
    public ApplicationRunner cargarUsuariosIniciales(

        JdbcTemplate jdbc,

        CredencialesService credencialesService,

        TransactionTemplate transactionTemplate,
        Environment entorno

    ) {

        return args -> {

            List<UsuarioInicial> usuarios = List.of(

                new UsuarioInicial(
                    1L,
                    1L,
                    "17.284.519-3",
                    "Matías Godoy Herrera",
                    "terreno@gmail.com"
                ),

                new UsuarioInicial(
                    2L,
                    1L,
                    "19.412.087-7",
                    "Javiera Tapia Muñoz",
                    "terreno2@gmail.com"
                ),

                new UsuarioInicial(
                    3L,
                    2L,
                    "15.906.334-8",
                    "Andrea Contreras Silva",
                    "ingeniero@gmail.com"
                ),

                new UsuarioInicial(
                    4L,
                    3L,
                    "13.871.905-7",
                    "Ricardo Espinoza Lagos",
                    "jefe@gmail.com"
                ),

                new UsuarioInicial(
                    5L,
                    4L,
                    "14.226.791-8",
                    "Marcela Fuentes Rojas",
                    "cliente@gmail.com"
                )
            );

            transactionTemplate.executeWithoutResult(
                status -> {

                    for (UsuarioInicial u : usuarios) {

                        var existentes = jdbc.queryForList(
                            """
                            SELECT id_usuario, correo, rut, id_rol
                            FROM usuario
                            WHERE id_usuario = ?
                            """,
                            u.id()
                        );

                        if (!existentes.isEmpty()) {

                            String correoExistente =
                                existentes.get(0)
                                    .get("correo")
                                    .toString();

                            var existente = existentes.getFirst();
                            boolean identidadCoincide = RutValidator.normalizar(u.rut()).equals(existente.get("rut"))
                                    && ((Number) existente.get("id_rol")).longValue() == u.rolId();
                            String correoAnterior = switch (u.id().intValue()) {
                                case 1 -> "mgodoy@induztek.ejemplo";
                                case 2 -> "jtapia@induztek.ejemplo";
                                case 3 -> "acontreras@induztek.ejemplo";
                                case 4 -> "respinoza@induztek.ejemplo";
                                case 5 -> "mfuentes@envpacifico.ejemplo";
                                default -> "";
                            };
                            if (identidadCoincide && correoExistente.equalsIgnoreCase(correoAnterior)) {
                                jdbc.update("UPDATE usuario SET correo = ? WHERE id_usuario = ?", u.correo(), u.id());
                            }

                            continue;
                        }

                        String temporal =
                            obtenerClaveTemporal(entorno, u.id());

                        String hash =
                            credencialesService
                                .codificarTemporal(temporal);

                        jdbc.update(
                            """
                            INSERT INTO usuario (
                                id_usuario,
                                id_rol,
                                rut,
                                nombre,
                                correo,
                                `contraseña`,
                                activo
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,

                            u.id(),

                            u.rolId(),

                            RutValidator.normalizar(u.rut()),

                            u.nombre(),

                            u.correo(),

                            hash,

                            true
                        );
                    }
                }
            );

        };
    }

    private String obtenerClaveTemporal(Environment entorno, Long id) {
        String variable = "DEMO_PASSWORD_" + id;
        String clave = entorno.getProperty(variable);
        if (clave == null || clave.isBlank()) return "clave";
        if (clave.getBytes(StandardCharsets.UTF_8).length < 16
                || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configura " + variable + " con una clave de 16 a 72 bytes");
        }
        return clave;
    }
}
