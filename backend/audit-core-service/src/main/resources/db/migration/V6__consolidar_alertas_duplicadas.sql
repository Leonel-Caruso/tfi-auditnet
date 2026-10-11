-- =====================================================================
-- AuditNet · audit-core-service · V6 — Consolidar alertas duplicadas (Bloque 5)
-- ---------------------------------------------------------------------
-- La migración V4 creó una alerta por cada hallazgo CRITICA/ALTA que ya
-- existía. Si un equipo se había auditado varias veces sin corregir,
-- quedaron varias alertas vigentes para el mismo problema (mismo
-- dispositivo y misma regla). Desde D-26 hay como máximo una.
--
-- Se conserva una por dispositivo y regla: primero la que está
-- EN_ATENCION (alguien la tomó), después la de mayor severidad y, a
-- igualdad, la más reciente. Las demás se cierran con un comentario que
-- indica cuál quedó vigente. No se borra nada.
-- =====================================================================

WITH vigentes AS (
    SELECT a.id_alerta,
           ROW_NUMBER() OVER ventana  AS orden,
           FIRST_VALUE(a.id_alerta) OVER ventana AS conservada
    FROM alertas_criticas a
    JOIN hallazgos_auditoria h ON h.id_hallazgo = a.id_hallazgo
    WHERE a.estado IN ('ACTIVA', 'EN_ATENCION')
    WINDOW ventana AS (
        PARTITION BY a.id_dispositivo, h.codigo_regla
        ORDER BY CASE a.estado WHEN 'EN_ATENCION' THEN 0 ELSE 1 END,
                 CASE a.severidad WHEN 'CRITICA' THEN 0 ELSE 1 END,
                 a.fecha_generacion DESC,
                 a.id_alerta DESC
    )
)
UPDATE alertas_criticas a
SET estado         = 'CERRADA',
    comentario     = 'Consolidada (V6): el mismo problema del equipo ya tiene la alerta ALR-'
                         || lpad(v.conservada::text, 3, '0') || '.',
    fecha_estado   = now(),
    usuario_estado = 'SISTEMA'
FROM vigentes v
WHERE a.id_alerta = v.id_alerta
  AND v.orden > 1;
