package cl.induztek.ms_equipos.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipo", uniqueConstraints = @UniqueConstraint(
        name = "uk_equipo_instalacion_tag", columnNames = {"instalacion", "tag"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipo {
    @Column(nullable = false)
    private boolean archivado;

    public void archivar() {
        archivado = true;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Long id;

    @Column(nullable = false, length = 60)
    private String tag;

    @Column(name = "tipo_equipo", nullable = false, length = 80)
    private String tipoEquipo;

    @Column(nullable = false, length = 80)
    private String marca;

    @Column(name = "numero_serie", nullable = false, length = 100)
    private String serie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalacion", referencedColumnName = "nombre", nullable = false)
    private Instalacion instalacion;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    public Equipo(String tag, String tipoEquipo, String marca, String serie, Instalacion instalacion) {
        this.tag = tag;
        this.tipoEquipo = tipoEquipo;
        this.marca = marca;
        this.serie = serie;
        this.instalacion = instalacion;
        this.idCliente = instalacion.getIdCliente();
    }
}
