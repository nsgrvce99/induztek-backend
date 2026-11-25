CREATE DATABASE IF NOT EXISTS induztek_instrumentos CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE induztek_instrumentos;

CREATE TABLE IF NOT EXISTS instrumento (
    id_instrumento BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    marca VARCHAR(120) NOT NULL,
    numero_serie VARCHAR(100) NOT NULL,
    fecha_vencimiento DATE NOT NULL
) ENGINE=InnoDB;

INSERT INTO instrumento (id_instrumento, nombre, marca, numero_serie, fecha_vencimiento) VALUES
    (1, 'Megohmetro 5 kV', 'Megger MIT525', 'MIT-0045821', '2027-03-15'),
    (2, 'Megohmetro 10 kV', 'Megger MIT1025', 'MIT-0071233', '2026-10-10'),
    (3, 'Medidor de relación (TTR)', 'Megger TTR300', 'TTR-300440', '2027-06-02'),
    (4, 'Micro-ohmímetro', 'Megger DLRO10X', 'DLR-10882', '2026-08-20'),
    (5, 'Equipo de inyección secundaria', 'Omicron CMC 356', 'CMC-HB411', '2027-01-05')
ON DUPLICATE KEY UPDATE id_instrumento = instrumento.id_instrumento;
