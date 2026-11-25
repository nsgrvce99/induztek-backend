package cl.induztek.ms_instrumentos.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instrumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instrumento")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String marca;

    @Column(name = "numero_serie", nullable = false, length = 100)
    private String serie;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    public Instrumento(String nombre, String marca, String serie, LocalDate fechaVencimiento) {
        actualizar(nombre, marca, serie, fechaVencimiento);
    }

    public void actualizar(String nombre, String marca, String serie, LocalDate fechaVencimiento) {
        this.nombre = nombre.trim();
        this.marca = marca.trim();
        this.serie = serie.trim();
        this.fechaVencimiento = fechaVencimiento;
    }
}
