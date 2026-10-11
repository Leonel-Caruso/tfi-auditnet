-- =====================================================================
-- AuditNet · management-service · V4 — Seguridad de cuentas (Bloque 6)
-- ---------------------------------------------------------------------
-- CU-001-001 y CU-001-002:
--   * intentos_fallidos / bloqueado_hasta: después de 5 contraseñas
--     incorrectas seguidas la cuenta queda bloqueada 15 minutos.
--   * ultimo_acceso: fecha del último inicio de sesión exitoso.
--   * sesiones_validas_desde: al cambiar la contraseña, los roles o la
--     organización de un usuario, los tokens emitidos antes dejan de valer
--     (los servicios lo verifican en cada solicitud).
-- =====================================================================

ALTER TABLE usuarios ADD COLUMN intentos_fallidos integer NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN bloqueado_hasta timestamp(6) with time zone;
ALTER TABLE usuarios ADD COLUMN ultimo_acceso timestamp(6) with time zone;
ALTER TABLE usuarios ADD COLUMN sesiones_validas_desde timestamp(6) with time zone;

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_intentos_fallidos CHECK (intentos_fallidos >= 0);

-- Descripciones de los cuatro perfiles fijos (D-29), alineadas con la tesis.
UPDATE roles SET descripcion = 'Administra usuarios, organizaciones, sedes, bitácoras y la salud de la plataforma.'
WHERE nombre = 'ADMINISTRADOR_SISTEMA';
UPDATE roles SET descripcion = 'Importa configuraciones, ejecuta auditorías, atiende hallazgos y deriva alertas.'
WHERE nombre = 'ANALISTA_RED';
UPDATE roles SET descripcion = 'Define baselines y reglas, revisa hallazgos y genera reportes de auditoría.'
WHERE nombre = 'AUDITOR_TECNICO';
UPDATE roles SET descripcion = 'Consulta el tablero, las alertas y los reportes de su organización y deriva alertas.'
WHERE nombre = 'RESPONSABLE_GESTION_IT';
