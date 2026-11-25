package cl.induztek.ms_equipos.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instalacion", uniqueConstraints = @UniqueConstraint(
        name = "uk_instalacion_nombre", columnNames = "nombre"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instalacion {
    @Column(nullable = false)
    private boolean archivada;

    public void archivar() {
        archivada = true;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instalacion")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    public Instalacion(String nombre, Long idCliente) {
        this.nombre = nombre;
        this.idCliente = idCliente;
    }
}
