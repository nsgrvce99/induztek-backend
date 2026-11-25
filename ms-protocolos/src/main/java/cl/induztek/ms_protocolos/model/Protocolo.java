package cl.induztek.ms_protocolos.model;

import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Resultado;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Protocolo {
    public enum Estado { BORRADOR, REGISTRADO_TERRENO, PRECAUCION, OBSERVADO, FORMALIZADO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_protocolo")
    private Long id;

    @Version
    private long version;

    @Column(name = "id_equipo", nullable = false)
    private long equipoId;

    @Column(name = "id_usuario_registra", nullable = false)
    private long tecnicoId;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal temperatura;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal humedad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Resultado resultadoGlobal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24)
    private Estado estado;

    @Column(length = 1000)
    private String motivoObservacion;

    @Column(length = 1000)
    private String comentarioVariacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "texto_mediciones")
    private java.util.Map<String, String> textoMediciones;

    public void conservarTexto(java.util.Map<String, String> texto) {
        this.textoMediciones = java.util.Map.copyOf(texto);
    }

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medicion> mediciones = new ArrayList<>();

    public Protocolo(long equipoId, long tecnicoId, LocalDateTime fechaHora,
                     BigDecimal temperatura, BigDecimal humedad, Resultado resultadoGlobal) {
        this.equipoId = equipoId;
        this.tecnicoId = tecnicoId;
        this.fechaHora = fechaHora;
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.resultadoGlobal = resultadoGlobal;
        this.estado = Estado.BORRADOR;
    }

    public void editar(BigDecimal temperatura, BigDecimal humedad, Resultado resultado) {
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.resultadoGlobal = resultado;
        mediciones.clear();
        if (estado != Estado.BORRADOR) estado = Estado.REGISTRADO_TERRENO;
    }

    public void anular() {
        estado = Estado.ANULADO;
    }

    public void cerrarBorrador() {
        estado = Estado.REGISTRADO_TERRENO;
    }

    public void formalizar(String comentario) {
        estado = Estado.FORMALIZADO;
        comentarioVariacion = comentario;
    }

    public void observar(String motivo) {
        estado = Estado.OBSERVADO;
        motivoObservacion = motivo;
    }

    void agregar(Medicion medicion) {
        mediciones.add(medicion);
    }
}
