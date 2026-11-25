package cl.induztek.ms_protocolos.dto;

import cl.induztek.ms_protocolos.model.Medicion;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class FormatoMediciones {
    private FormatoMediciones() {}

    public static Map<String, String> conservar(BigDecimal temperatura, BigDecimal humedad, Medicion.Valores valores,
                                                Map<String, String> textoIngresado) {
        Map<String, BigDecimal> numeros = new LinkedHashMap<>();
        numeros.put("temperatura", temperatura);
        numeros.put("humedad", humedad);
        numeros.put("lectura1Min", valores.lectura1Min());
        numeros.put("lectura10Min", valores.lectura10Min());
        numeros.put("corrienteAjuste", valores.corrienteAjuste());
        numeros.put("corrienteOperacion", valores.corrienteOperacion());
        numeros.put("tiempoEsperado", valores.tiempoEsperado());
        numeros.put("tiempoMedido", valores.tiempoMedido());
        numeros.put("lecturaDevanadoA", valores.lecturaDevanadoA());
        numeros.put("lecturaDevanadoB", valores.lecturaDevanadoB());
        numeros.put("lecturaDevanadoC", valores.lecturaDevanadoC());

        Map<String, String> formato = new LinkedHashMap<>();
        numeros.forEach((campo, valor) -> {
            if (valor != null) formato.put(campo, valor.toPlainString());
        });
        if (textoIngresado != null) textoIngresado.forEach((campo, texto) -> {
            BigDecimal numero = numeros.get(campo);
            if (numero == null || texto == null || texto.length() > 32
                    || !texto.matches("-?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)")
                    || new BigDecimal(texto).compareTo(numero) != 0) {
                throw new ResponseStatusException(BAD_REQUEST, "El texto de " + campo + " no coincide con su valor");
            }
            formato.put(campo, texto);
        });
        return formato;
    }
}