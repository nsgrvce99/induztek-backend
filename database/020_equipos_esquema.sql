CREATE DATABASE IF NOT EXISTS induztek_equipos CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE induztek_equipos;

CREATE TABLE IF NOT EXISTS instalacion (
    id_instalacion BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    id_cliente BIGINT NOT NULL,
    archivada BIT NOT NULL DEFAULT b'0',
    CONSTRAINT uk_instalacion_nombre UNIQUE (nombre),
    CONSTRAINT uk_instalacion_nombre_cliente UNIQUE (nombre, id_cliente),
    INDEX ix_instalacion_cliente (id_cliente)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipo (
    id_equipo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tag VARCHAR(60) NOT NULL,
    tipo_equipo VARCHAR(80) NOT NULL,
    marca VARCHAR(80) NOT NULL,
    numero_serie VARCHAR(100) NOT NULL,
    instalacion VARCHAR(120) NOT NULL,
    id_cliente BIGINT NOT NULL,
    archivado BIT NOT NULL DEFAULT b'0',
    CONSTRAINT uk_equipo_instalacion_tag UNIQUE (instalacion, tag),
    CONSTRAINT fk_equipo_instalacion_cliente FOREIGN KEY (instalacion, id_cliente)
        REFERENCES instalacion (nombre, id_cliente),
    INDEX ix_equipo_cliente (id_cliente)
) ENGINE=InnoDB;
