package cl.induztek.ms_usuarios.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public final class FotoPerfilValidator {
    private static final String PREFIJO = "data:image/jpeg;base64,";
    private static final int MAXIMO_URI = 2_000;
    private static final int MAXIMO_DATO = 44_000;
    private static final int MAXIMO_LADO = 512;

    private FotoPerfilValidator() {}

    public static void validar(String foto) {
        if (foto == null) return;
        if (!foto.startsWith("data:")) {
            if (foto.length() > MAXIMO_URI) throw fotoInvalida();
            return;
        }
        if (!foto.startsWith(PREFIJO) || foto.length() > MAXIMO_DATO) throw fotoInvalida();

        try {
            byte[] bytes = Base64.getDecoder().decode(foto.substring(PREFIJO.length()));
            try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var lectores = ImageIO.getImageReaders(entrada);
                if (!lectores.hasNext()) throw fotoInvalida();
                var lector = lectores.next();
                try {
                    lector.setInput(entrada, true, true);
                    if (!"JPEG".equalsIgnoreCase(lector.getFormatName())
                            || lector.getWidth(0) > MAXIMO_LADO || lector.getHeight(0) > MAXIMO_LADO) {
                        throw fotoInvalida();
                    }
                    lector.read(0);
                } finally {
                    lector.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            throw fotoInvalida();
        }
    }

    private static ResponseStatusException fotoInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser un JPEG válido de hasta 512 píxeles por lado");
    }
}
