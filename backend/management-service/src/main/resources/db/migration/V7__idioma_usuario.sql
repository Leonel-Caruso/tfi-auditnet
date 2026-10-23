-- =====================================================================
-- AuditNet · management-service · V7 — Idioma del usuario
-- ---------------------------------------------------------------------
-- Idioma de la consola y de los correos de cada usuario: español (es),
-- inglés (en) o portugués de Brasil (pt). Los usuarios existentes y los
-- nuevos quedan en español hasta que elijan otro en Mi cuenta.
-- =====================================================================

ALTER TABLE usuarios ADD COLUMN idioma varchar(2) NOT NULL DEFAULT 'es';
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_idioma CHECK (idioma IN ('es', 'en', 'pt'));
