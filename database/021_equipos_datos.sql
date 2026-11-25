USE induztek_equipos;
START TRANSACTION;

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 1, 'Planta Lampa', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 1 OR nombre = 'Planta Lampa');

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 2, 'Planta Elevadora Sur', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 2 OR nombre = 'Planta Elevadora Sur');

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 3, 'Faena Til Til', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 3 OR nombre = 'Faena Til Til');

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 4, 'Edificio Clínico', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 4 OR nombre = 'Edificio Clínico');

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 5, 'Planta Molienda', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 5 OR nombre = 'Planta Molienda');

INSERT INTO instalacion (id_instalacion, nombre, id_cliente)
SELECT 6, 'Site Huechuraba', 5
WHERE NOT EXISTS (SELECT 1 FROM instalacion WHERE id_instalacion = 6 OR nombre = 'Site Huechuraba');

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 1, 'TR-01', 'Transformador', 'Rhona', 'RH-18733', 'Planta Lampa', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Planta Lampa' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 1 OR (instalacion = 'Planta Lampa' AND tag = 'TR-01'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 2, 'TR-02', 'Transformador', 'Rhona', 'RH-21904', 'Planta Lampa', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Planta Lampa' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 2 OR (instalacion = 'Planta Lampa' AND tag = 'TR-02'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 3, 'TR-A', 'Transformador', 'Tusan', 'TS-5521', 'Planta Elevadora Sur', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Planta Elevadora Sur' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 3 OR (instalacion = 'Planta Elevadora Sur' AND tag = 'TR-A'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 4, 'TR-SS-01', 'Transformador', 'ABB', 'ABB-1180442', 'Faena Til Til', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Faena Til Til' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 4 OR (instalacion = 'Faena Til Til' AND tag = 'TR-SS-01'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 5, 'INT-52-1', 'Interruptor', 'Schneider', 'SE-449021', 'Faena Til Til', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Faena Til Til' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 5 OR (instalacion = 'Faena Til Til' AND tag = 'INT-52-1'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 6, 'CEL-MT-03', 'Celda MT', 'Ormazabal', 'OR-77812', 'Edificio Clínico', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Edificio Clínico' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 6 OR (instalacion = 'Edificio Clínico' AND tag = 'CEL-MT-03'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 7, 'REL-50/51-A', 'Relé de protección', 'SEL', 'SEL-751-30219', 'Planta Molienda', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Planta Molienda' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 7 OR (instalacion = 'Planta Molienda' AND tag = 'REL-50/51-A'));

INSERT INTO equipo (id_equipo, tag, tipo_equipo, marca, numero_serie, instalacion, id_cliente)
SELECT 8, 'CAB-MT-02', 'Cable de poder', 'Nexans', 'NX-2201-B', 'Site Huechuraba', 5
WHERE EXISTS (SELECT 1 FROM instalacion WHERE nombre = 'Site Huechuraba' AND id_cliente = 5)
  AND NOT EXISTS (SELECT 1 FROM equipo WHERE id_equipo = 8 OR (instalacion = 'Site Huechuraba' AND tag = 'CAB-MT-02'));

COMMIT;
