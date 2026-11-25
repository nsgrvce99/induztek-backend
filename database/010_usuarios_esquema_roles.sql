SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS induztek_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE induztek_usuarios;

CREATE TABLE IF NOT EXISTS rol (
    id_rol BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_rol BIGINT NOT NULL,
    rut VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    `contraseña` VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    activo BIT NOT NULL DEFAULT b'1',
    archivado BIT NOT NULL DEFAULT b'0',
    giro VARCHAR(150),
    contacto_nombre VARCHAR(150),
    contacto_cargo VARCHAR(100),
    foto_uri TEXT,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE=InnoDB;

INSERT INTO rol (id_rol, codigo, nombre) VALUES
    (1, 'TECNICO', 'Técnico de terreno'),
    (2, 'INGENIERO', 'Ingeniero de pruebas'),
    (3, 'JEFE_TECNICO', 'Jefe técnico'),
    (4, 'CLIENTE', 'Usuario cliente')
ON DUPLICATE KEY UPDATE id_rol = rol.id_rol;