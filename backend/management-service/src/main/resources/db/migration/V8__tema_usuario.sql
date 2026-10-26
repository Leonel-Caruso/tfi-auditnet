-- =====================================================================
-- AuditNet · management-service · V8 — Tema del usuario
-- ---------------------------------------------------------------------
-- Tema de la consola que eligió cada usuario: claro u oscuro. Vacío
-- mientras no elija uno: la consola usa el del navegador o del sistema.
-- =====================================================================

ALTER TABLE usuarios ADD COLUMN tema varchar(6);
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_tema CHECK (tema IS NULL OR tema IN ('claro', 'oscuro'));
