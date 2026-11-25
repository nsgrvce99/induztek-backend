package cl.induztek.ms_protocolos;

import cl.induztek.ms_protocolos.service.EvaluacionPrueba;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Resultado;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Tipo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluacionPruebaTest {
    @ParameterizedTest
    @CsvSource({"100,125,false", "100,125.0001,true", "100,74.99,true", "0,100,false"})
    void exigeComentarioSoloAlSuperarElUmbral(BigDecimal anterior, BigDecimal actual, boolean esperado) {
        assertEquals(esperado, EvaluacionPrueba.variacionSignificativa(anterior, actual));
    }


    @ParameterizedTest
    @CsvSource({
            "AISLACION,100,200,APROBADO",
            "AISLACION,100,100,PRECAUCION",
            "AISLACION,100,99.9,RECHAZADO",
            "AISLACION,0,200,PENDIENTE",
            "TTR,100,100.5,APROBADO",
            "TTR,100,101,PRECAUCION",
            "TTR,100,101.0001,RECHAZADO",
            "RELE,100,95,APROBADO",
            "RELE,100,90,PRECAUCION",
            "RELE,100,89.9,RECHAZADO",
            "CONTACTOS,100,100,APROBADO",
            "CONTACTOS,120,100,PRECAUCION",
            "CONTACTOS,120.01,100,RECHAZADO",
            "DEVANADOS,0,0,REVISION_MANUAL"
    })
    void clasificaEnElUmbral(Tipo tipo, BigDecimal referencia, BigDecimal lectura, Resultado esperado) {
        assertEquals(esperado, EvaluacionPrueba.evaluar(tipo, referencia, lectura).resultado());
    }
}
