package cl.induztek.ms_usuarios.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class CredencialesService {

    private static final String TEMPORAL = "{temporal}";

    private final PasswordEncoder passwordEncoder;

    public CredencialesService(
        PasswordEncoder passwordEncoder
    ) {

        this.passwordEncoder = passwordEncoder;
    }

    public boolean requiereCambio(String hash) {

        return hash != null
            && hash.startsWith(TEMPORAL);
    }

    public boolean verificar(
        String contrasena,
        String hash
    ) {

        if (contrasena == null || hash == null) {
            return false;
        }

        if (contrasena.getBytes(
                StandardCharsets.UTF_8
            ).length > 72) {

            return false;
        }

        String hashReal = requiereCambio(hash)
            ? hash.substring(TEMPORAL.length())
            : hash;

        return passwordEncoder.matches(
            contrasena,
            hashReal
        );
    }

    public String codificarTemporal(String contrasena) {

        return TEMPORAL
            + passwordEncoder.encode(contrasena);
    }

    public String codificar(String contrasena) {

        return passwordEncoder.encode(contrasena);
    }
}