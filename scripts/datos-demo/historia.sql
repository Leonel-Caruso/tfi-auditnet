-- Reparte en las últimas 8 semanas las auditorías que cargó cargar.py (todas quedan con la fecha de la carga).
--
-- Solo toca las organizaciones de demostración y las auditorías de las últimas 24 horas, así se puede volver a
-- ejecutar sin mover dos veces lo mismo. Por equipo, la primera versión de configuración queda unas 8 semanas atrás
-- y la última a pocos días; un equipo con una sola versión queda con más de 30 días (configuración desactualizada).
-- Lo que pasó después de cada auditoría (hallazgos, cambios de estado, alertas) se mueve con ella.
--
-- Las bitácoras y el catálogo de archivos no se tocan: conservan la fecha real de la carga.

BEGIN;

CREATE TEMP TABLE demo_auditorias ON COMMIT DROP AS
SELECT a.id_auditoria,
       a.id_dispositivo,
       a.fecha_ejecucion AS original,
       row_number() OVER (PARTITION BY a.id_dispositivo ORDER BY a.fecha_ejecucion) AS orden,
       count(*) OVER (PARTITION BY a.id_dispositivo) AS total
FROM auditorias_configuracion a
JOIN organizaciones_cliente o ON o.id_organizacion = a.id_organizacion
WHERE o.identificador IN ('COOP-LITORAL', 'BODEGA-CERRO-ALTO', 'AGRO-PAMPA', 'SALUD-AUSTRAL', 'LOG-MERIDIANO',
                          'EDU-VALLE-NORTE')
  AND a.fecha_ejecucion > now() - interval '1 day';

-- Días hacia atrás: la primera versión entre 42 y 56, la última entre 2 y 13, y las del medio repartidas en partes
-- iguales. Con una sola versión, entre 33 y 49.
ALTER TABLE demo_auditorias ADD COLUMN inicio int, ADD COLUMN fin int, ADD COLUMN nueva timestamptz;
UPDATE demo_auditorias
SET inicio = CASE WHEN total = 1 THEN 33 + (id_dispositivo % 5) * 4 ELSE 42 + (id_dispositivo * 5) % 15 END,
    fin = 2 + (id_dispositivo * 7) % 12;
UPDATE demo_auditorias
SET nueva = date_trunc('day', now())
            - make_interval(days => CASE WHEN total = 1 THEN inicio
                                         ELSE round(inicio - (orden - 1) * (inicio - fin)::numeric / (total - 1))::int
                                    END)
            + make_interval(hours => (12 + id_dispositivo % 7)::int, mins => ((id_auditoria * 13) % 60)::int);

ALTER TABLE demo_auditorias ADD COLUMN corrimiento interval;
UPDATE demo_auditorias SET corrimiento = nueva - original;

-- Lo ocurrido en el instante t se corre igual que la última auditoría ejecutada antes de t.
CREATE FUNCTION pg_temp.corrida(t timestamptz) RETURNS timestamptz LANGUAGE sql STABLE AS $$
    SELECT t + coalesce((SELECT corrimiento FROM demo_auditorias WHERE original <= t ORDER BY original DESC LIMIT 1),
                        interval '0')
$$;

UPDATE configuraciones_dispositivo c
SET fecha_importacion = d.nueva - interval '25 minutes'
FROM auditorias_configuracion a
JOIN demo_auditorias d ON d.id_auditoria = a.id_auditoria
WHERE c.id_configuracion = a.id_configuracion;

UPDATE auditorias_configuracion a
SET fecha_ejecucion = d.nueva
FROM demo_auditorias d
WHERE a.id_auditoria = d.id_auditoria;

UPDATE hallazgos_auditoria h
SET fecha_deteccion = d.nueva,
    fecha_estado = pg_temp.corrida(h.fecha_estado)
FROM demo_auditorias d
WHERE h.id_auditoria = d.id_auditoria;

UPDATE alertas_criticas al
SET fecha_generacion = pg_temp.corrida(al.fecha_generacion),
    fecha_estado = pg_temp.corrida(al.fecha_estado)
FROM demo_auditorias d
WHERE al.id_auditoria = d.id_auditoria;

-- El historial de cada hallazgo es de solo inserción: se habilita el cambio solo dentro de esta transacción.
ALTER TABLE seguimientos_hallazgo DISABLE TRIGGER tr_seguimientos_hallazgo_sin_cambios;
UPDATE seguimientos_hallazgo s
SET fecha = pg_temp.corrida(s.fecha)
FROM hallazgos_auditoria h
JOIN demo_auditorias d ON d.id_auditoria = h.id_auditoria
WHERE s.id_hallazgo = h.id_hallazgo;
ALTER TABLE seguimientos_hallazgo ENABLE TRIGGER tr_seguimientos_hallazgo_sin_cambios;

SELECT count(*) AS auditorias_repartidas,
       min(nueva)::date AS primera,
       max(nueva)::date AS ultima
FROM demo_auditorias;

COMMIT;
