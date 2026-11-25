package cl.induztek.ms_protocolos.model;

import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Tipo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_protocolo", nullable = false)
    private Protocolo protocolo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24)
    private Tipo tipoPrueba;

    @Column(name = "id_instrumento", nullable = false)
    private long instrumentoId;

    @Column(name = "lectura_1min", precision = 24, scale = 8)
    private BigDecimal lectura1Min;

    @Column(name = "lectura_10min", precision = 24, scale = 8)
    private BigDecimal lectura10Min;

    @Column(precision = 24, scale = 8)
    private BigDecimal indicePolarizacion;

    @Column(precision = 24, scale = 8)
    private BigDecimal corrienteAjuste;

    @Column(precision = 24, scale = 8)
    private BigDecimal corrienteOperacion;

    @Column(precision = 24, scale = 8)
    private BigDecimal tiempoEsperado;

    @Column(precision = 24, scale = 8)
    private BigDecimal tiempoMedido;

    @Column(name = "lectura_devanado_a", precision = 24, scale = 8)
    private BigDecimal lecturaDevanadoA;

    @Column(name = "lectura_devanado_b", precision = 24, scale = 8)
    private BigDecimal lecturaDevanadoB;

    @Column(name = "lectura_devanado_c", precision = 24, scale = 8)
    private BigDecimal lecturaDevanadoC;

    public Medicion(Protocolo protocolo, Tipo tipoPrueba, long instrumentoId, Valores valores,
                    BigDecimal indicePolarizacion) {
        this.protocolo = protocolo;
        this.tipoPrueba = tipoPrueba;
        this.instrumentoId = instrumentoId;
        this.lectura1Min = valores.lectura1Min();
        this.lectura10Min = valores.lectura10Min();
        this.indicePolarizacion = indicePolarizacion;
        this.corrienteAjuste = valores.corrienteAjuste();
        this.corrienteOperacion = valores.corrienteOperacion();
        this.tiempoEsperado = valores.tiempoEsperado();
        this.tiempoMedido = valores.tiempoMedido();
        this.lecturaDevanadoA = valores.lecturaDevanadoA();
        this.lecturaDevanadoB = valores.lecturaDevanadoB();
        this.lecturaDevanadoC = valores.lecturaDevanadoC();
        protocolo.agregar(this);
    }

    public record Valores(
            @Digits(integer = 16, fraction = 8) BigDecimal lectura1Min,
            @Digits(integer = 16, fraction = 8) BigDecimal lectura10Min,
            @Digits(integer = 16, fraction = 8) BigDecimal corrienteAjuste,
            @Digits(integer = 16, fraction = 8) BigDecimal corrienteOperacion,
            @Digits(integer = 16, fraction = 8) BigDecimal tiempoEsperado,
            @Digits(integer = 16, fraction = 8) BigDecimal tiempoMedido,
            @Digits(integer = 16, fraction = 8) BigDecimal lecturaDevanadoA,
            @Digits(integer = 16, fraction = 8) BigDecimal lecturaDevanadoB,
            @Digits(integer = 16, fraction = 8) BigDecimal lecturaDevanadoC) {}
}
