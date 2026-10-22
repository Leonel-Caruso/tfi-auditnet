-- =====================================================================
-- AuditNet · audit-core-service · V8 — Revisión periódica y vencimientos
-- ---------------------------------------------------------------------
-- * politicas_revision: plazos de corrección por severidad, cada cuántos
--   días se vuelve a auditar un equipo y a partir de cuántos días una
--   configuración se considera desactualizada. Una organización sin fila
--   usa los valores por defecto (7/15/30/60, 30 y 30 días).
-- * hallazgos_auditoria.aviso_vencimiento_en y
--   dispositivos_red.aviso_configuracion_en: cuándo se avisó por correo,
--   para no repetir el aviso todos los días.
-- * auditorias_configuracion.origen: MANUAL o AUTOMATICA (la ejecuta la
--   revisión periódica).
-- * resumenes_mensuales: un resumen por organización y mes.
-- =====================================================================

CREATE TABLE politicas_revision (
    id_organizacion        bigint                      NOT NULL,
    dias_critica           integer                     NOT NULL DEFAULT 7,
    dias_alta              integer                     NOT NULL DEFAULT 15,
    dias_media             integer                     NOT NULL DEFAULT 30,
    dias_baja              integer                     NOT NULL DEFAULT 60,
    dias_reauditoria       integer                     NOT NULL DEFAULT 30,
    dias_configuracion     integer                     NOT NULL DEFAULT 30,
    reauditoria_automatica boolean                     NOT NULL DEFAULT true,
    modificada_en          timestamp(6) with time zone NOT NULL,
    modificada_por         varchar(120)                NOT NULL,
    CONSTRAINT politicas_revision_pkey PRIMARY KEY (id_organizacion),
    CONSTRAINT fk_politicas_revision_organizacion
        FOREIGN KEY (id_organizacion) REFERENCES organizaciones_cliente (id_organizacion),
    CONSTRAINT ck_politicas_revision_dias CHECK (
        dias_critica BETWEEN 1 AND 365 AND dias_alta BETWEEN 1 AND 365 AND dias_media BETWEEN 1 AND 365
        AND dias_baja BETWEEN 1 AND 365 AND dias_reauditoria BETWEEN 1 AND 365
        AND dias_configuracion BETWEEN 1 AND 365),
    CONSTRAINT ck_politicas_revision_orden CHECK (dias_critica <= dias_alta AND dias_alta <= dias_media
        AND dias_media <= dias_baja)
);

ALTER TABLE hallazgos_auditoria ADD COLUMN aviso_vencimiento_en timestamp(6) with time zone;
ALTER TABLE dispositivos_red ADD COLUMN aviso_configuracion_en timestamp(6) with time zone;

ALTER TABLE auditorias_configuracion ADD COLUMN origen varchar(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE auditorias_configuracion
    ADD CONSTRAINT ck_auditorias_configuracion_origen CHECK (origen IN ('MANUAL', 'AUTOMATICA'));

CREATE TABLE resumenes_mensuales (
    id_organizacion bigint                      NOT NULL,
    periodo         char(7)                     NOT NULL,
    enviado_en      timestamp(6) with time zone NOT NULL,
    destinatarios   integer                     NOT NULL,
    CONSTRAINT resumenes_mensuales_pkey PRIMARY KEY (id_organizacion, periodo),
    CONSTRAINT fk_resumenes_mensuales_organizacion
        FOREIGN KEY (id_organizacion) REFERENCES organizaciones_cliente (id_organizacion),
    CONSTRAINT ck_resumenes_mensuales_periodo CHECK (periodo ~ '^[0-9]{4}-[0-9]{2}$')
);
