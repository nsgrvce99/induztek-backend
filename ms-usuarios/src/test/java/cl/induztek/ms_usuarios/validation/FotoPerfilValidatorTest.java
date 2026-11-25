package cl.induztek.ms_usuarios.validation;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FotoPerfilValidatorTest {
    @Test
    void aceptaJpegCompactoYConservaReferenciasExistentes() throws IOException {
        assertDoesNotThrow(() -> FotoPerfilValidator.validar(foto(32)));
        assertDoesNotThrow(() -> FotoPerfilValidator.validar("content://perfil/1"));
        assertDoesNotThrow(() -> FotoPerfilValidator.validar(null));
    }

    @Test
    void rechazaContenidoInvalidoOImagenExcesiva() throws IOException {
        assertThrows(ResponseStatusException.class, () -> FotoPerfilValidator.validar(foto(513)));
        assertThrows(ResponseStatusException.class, () -> FotoPerfilValidator.validar("data:image/png;base64,eA=="));
        assertThrows(ResponseStatusException.class, () -> FotoPerfilValidator.validar("data:image/jpeg;base64,eA=="));
    }

    private static String foto(int lado) throws IOException {
        var imagen = new BufferedImage(lado, 16, BufferedImage.TYPE_INT_RGB);
        var salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "JPEG", salida);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
    }
}
