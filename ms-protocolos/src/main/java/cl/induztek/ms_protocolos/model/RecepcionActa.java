package cl.induztek.ms_protocolos.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecepcionActa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recepcion")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_protocolo", nullable = false, unique = true)
    private Protocolo protocolo;

    @Column(nullable = false, length = 120)
    private String nombreReceptor;

    @Column(nullable = false, length = 16)
    private String rutReceptor;

    @Column(nullable = false, length = 120)
    private String cargoReceptor;

    @Column(nullable = false)
    private LocalDateTime fechaHoraRecepcion;

    public RecepcionActa(Protocolo protocolo, String nombreReceptor, String rutReceptor,
                         String cargoReceptor, LocalDateTime fechaHoraRecepcion) {
        this.protocolo = protocolo;
        this.nombreReceptor = nombreReceptor;
        this.rutReceptor = rutReceptor;
        this.cargoReceptor = cargoReceptor;
        this.fechaHoraRecepcion = fechaHoraRecepcion;
    }
}
