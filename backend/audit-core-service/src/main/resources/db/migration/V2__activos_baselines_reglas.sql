-- =====================================================================
-- AuditNet · audit-core-service · V2 — Activos, baselines y reglas (Bloque 3)
-- ---------------------------------------------------------------------
-- 1. Dispositivos: el identificador pasa a ser único POR ORGANIZACIÓN
--    (plataforma B2B multicliente: dos clientes pueden tener un equipo
--    llamado igual). Los datos existentes ya cumplen la regla nueva,
--    porque la anterior (única global) era más estricta.
-- 2. Baselines: se permite versionar. La unicidad pasa de
--    (organización, nombre) a (organización, nombre, versión).
-- 3. Reglas: tipo VALOR_ESPERADO (parámetro + valor esperado) y campo
--    impacto (RF-006). Los CHECK de las tablas que guardan una copia del
--    tipo de regla (evaluaciones y hallazgos) se amplían igual.
-- 4. Hallazgos: guardan una copia del impacto de la regla (RF-009).
--
-- Sin pérdida de datos: solo se agregan columnas y se reemplazan
-- restricciones por otras compatibles con los datos existentes.
-- =====================================================================

-- 1. Identificador de dispositivo único por organización ---------------
ALTER TABLE dispositivos_red
    DROP CONSTRAINT uk_dispositivos_red_identificador;
ALTER TABLE dispositivos_red
    ADD CONSTRAINT uk_dispositivos_red_org_identificador UNIQUE (id_organizacion, identificador);

-- 2. Versionado de baselines -------------------------------------------
ALTER TABLE baselines_configuracion
    DROP CONSTRAINT uk_baseline_org_nombre;
ALTER TABLE baselines_configuracion
    ADD CONSTRAINT uk_baseline_org_nombre_version UNIQUE (id_organizacion, nombre, version);
ALTER TABLE baselines_configuracion
    ADD CONSTRAINT ck_baselines_configuracion_version CHECK (version >= 1);

-- 3. Reglas: valor esperado e impacto ----------------------------------
ALTER TABLE reglas_baseline
    ADD COLUMN valor_esperado varchar(500);
ALTER TABLE reglas_baseline
    ADD COLUMN impacto varchar(500) NOT NULL DEFAULT 'Impacto no documentado (regla anterior a la Entrega 2).';
-- El valor por defecto solo completa las reglas existentes; las nuevas deben informar su impacto.
ALTER TABLE reglas_baseline
    ALTER COLUMN impacto DROP DEFAULT;

ALTER TABLE reglas_baseline
    DROP CONSTRAINT reglas_baseline_tipo_check;
ALTER TABLE reglas_baseline
    ADD CONSTRAINT reglas_baseline_tipo_check
        CHECK (tipo IN ('DEBE_CONTENER', 'NO_DEBE_CONTENER', 'VALOR_ESPERADO'));
-- VALOR_ESPERADO exige el valor; los otros tipos no lo usan.
ALTER TABLE reglas_baseline
    ADD CONSTRAINT ck_reglas_baseline_valor_esperado
        CHECK ((tipo = 'VALOR_ESPERADO') = (valor_esperado IS NOT NULL));

ALTER TABLE evaluaciones_regla_auditoria
    DROP CONSTRAINT evaluaciones_regla_auditoria_tipo_check;
ALTER TABLE evaluaciones_regla_auditoria
    ADD CONSTRAINT evaluaciones_regla_auditoria_tipo_check
        CHECK (tipo IN ('DEBE_CONTENER', 'NO_DEBE_CONTENER', 'VALOR_ESPERADO'));

ALTER TABLE hallazgos_auditoria
    DROP CONSTRAINT hallazgos_auditoria_tipo_regla_check;
ALTER TABLE hallazgos_auditoria
    ADD CONSTRAINT hallazgos_auditoria_tipo_regla_check
        CHECK (tipo_regla IN ('DEBE_CONTENER', 'NO_DEBE_CONTENER', 'VALOR_ESPERADO'));

-- 4. Copia del impacto en el hallazgo (null en hallazgos anteriores) ---
ALTER TABLE hallazgos_auditoria
    ADD COLUMN impacto varchar(500);
