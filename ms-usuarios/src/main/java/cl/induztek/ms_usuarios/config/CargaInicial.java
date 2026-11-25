package cl.induztek.ms_usuarios.config;

import cl.induztek.ms_usuarios.security.CredencialesService;
import cl.induztek.ms_usuarios.validation.RutValidator;

import org.springframework.boot.ApplicationRunner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
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

        TransactionTemplate transactionTemplate

    ) {

        return args -> {

            List<UsuarioInicial> usuarios = List.of(

                new UsuarioInicial(
                    1L,
                    1L,
                    "17.284.519-3",
                    "Matías Godoy Herrera",
                    "mgodoy@induztek.ejemplo"
                ),

                new UsuarioInicial(
                    2L,
                    1L,
                    "19.412.087-7",
                    "Javiera Tapia Muñoz",
                    "jtapia@induztek.ejemplo"
                ),

                new UsuarioInicial(
                    3L,
                    2L,
                    "15.906.334-8",
                    "Andrea Contreras Silva",
                    "acontreras@induztek.ejemplo"
                ),

                new UsuarioInicial(
                    4L,
                    3L,
                    "13.871.905-7",
                    "Ricardo Espinoza Lagos",
                    "respinoza@induztek.ejemplo"
                ),

                new UsuarioInicial(
                    5L,
                    4L,
                    "14.226.791-8",
                    "Marcela Fuentes Rojas",
                    "mfuentes@envpacifico.ejemplo"
                )
            );

            List<String> credenciales = new ArrayList<>();

            transactionTemplate.executeWithoutResult(
                status -> {

                    for (UsuarioInicial u : usuarios) {

                        var existentes = jdbc.queryForList(
                            """
                            SELECT id_usuario, correo
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

                            if (!correoExistente
                                    .equalsIgnoreCase(
                                        u.correo()
                                    )) {

                                throw new IllegalStateException(
                                    "El ID " + u.id()
                                    + " pertenece a otro usuario. "
                                    + "Revise MySQL antes de continuar."
                                );
                            }

                            continue;
                        }

                        String temporal =
                            generarClaveTemporal();

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

                        credenciales.add(
                            u.correo()
                            + " | "
                            + temporal
                        );
                    }
                }
            );

            // Se muestran solo después de confirmar
            // la transacción.

            if (!credenciales.isEmpty()) {

                System.out.println(
                    "=== CREDENCIALES TEMPORALES DE DEMO ==="
                );

                credenciales.forEach(System.out::println);

                System.out.println(
                    "====================================="
                );

            } else {

                System.out.println(
                    "Usuarios iniciales ya existentes."
                );
            }
        };
    }

    private String generarClaveTemporal() {

        byte[] bytes = new byte[12];

        new SecureRandom().nextBytes(bytes);

        return "A!" + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }
}