package ar.edu.uai.tfi.auditcore.infrastructure.backup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Genera {@code restaurar.sql}, el guion de psql que vuelve a cargar los datos del respaldo. Se ejecuta con el dueño
 * de las tablas, desde la carpeta del respaldo, sobre una base con la misma versión de esquema. Corre en una sola
 * transacción y desactiva los triggers de solo inserción de las bitácoras únicamente mientras restaura.
 */
public final class GuionRestauracion {

    private GuionRestauracion() {
    }

    public static String generar(String nombreRespaldo, EstructuraBase estructura, Map<String, String> versionesEsquema) {
        List<String> tablas = estructura.ordenRestauracion();
        StringBuilder sql = new StringBuilder();
        sql.append("-- AuditNet · restauración del respaldo ").append(nombreRespaldo).append('\n')
                .append("-- Uso (en la carpeta del respaldo descomprimido):\n")
                .append("--   psql \"host=... dbname=... user=... sslmode=require\" -f restaurar.sql\n")
                .append("-- Reemplaza TODOS los datos de la base por los del respaldo, en una sola transacción.\n\n")
                .append("\\set ON_ERROR_STOP on\n")
                .append("\\encoding UTF8\n")
                .append("BEGIN;\n\n")
                .append("-- 1. La base tiene que estar en la misma versión de esquema que el respaldo.\n")
                .append("DO $$\nDECLARE v text;\nBEGIN\n");
        versionesEsquema.forEach((tabla, version) -> sql
                .append("  IF to_regclass('").append(tabla).append("') IS NULL THEN\n")
                .append("    RAISE EXCEPTION 'Falta la tabla ").append(tabla)
                .append(": iniciá los servicios una vez para que Flyway cree el esquema.';\n")
                .append("  END IF;\n")
                .append("  SELECT version INTO v FROM ").append(tabla)
                .append(" WHERE success ORDER BY installed_rank DESC LIMIT 1;\n")
                .append("  IF v IS DISTINCT FROM '").append(version).append("' THEN\n")
                .append("    RAISE EXCEPTION 'La base está en la versión % de ").append(tabla)
                .append(" y el respaldo es de la versión ").append(version)
                .append(". Desplegá la misma versión de AuditNet antes de restaurar.', coalesce(v, '(ninguna)');\n")
                .append("  END IF;\n"));
        sql.append("END $$;\n\n");

        sql.append("-- 2. Triggers de solo inserción desactivados durante la restauración.\n");
        tablas.forEach(tabla -> sql.append("ALTER TABLE ").append(id(tabla)).append(" DISABLE TRIGGER USER;\n"));

        sql.append("\n-- 3. Se vacían las tablas y se cargan los datos del respaldo (primero las referenciadas).\n")
                .append("TRUNCATE TABLE ")
                .append(tablas.stream().map(GuionRestauracion::id).collect(Collectors.joining(", ")))
                .append(" RESTART IDENTITY CASCADE;\n");
        for (String tabla : tablas) {
            String columnas = estructura.columnas().get(tabla).stream().map(GuionRestauracion::id)
                    .collect(Collectors.joining(", "));
            sql.append("\\copy ").append(id(tabla)).append(" (").append(columnas).append(") FROM 'datos/")
                    .append(tabla).append(".csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')\n");
        }

        sql.append("\n-- 4. Las secuencias siguen desde el último id restaurado.\n");
        for (String tabla : tablas) {
            for (String columna : estructura.secuencias().getOrDefault(tabla, List.of())) {
                sql.append("SELECT setval(pg_get_serial_sequence('").append(id(tabla)).append("', '").append(columna)
                        .append("'), coalesce(max(").append(id(columna)).append("), 0) + 1, false) FROM ")
                        .append(id(tabla)).append(";\n");
            }
        }

        sql.append("\n-- 5. Se vuelven a activar los triggers.\n");
        tablas.forEach(tabla -> sql.append("ALTER TABLE ").append(id(tabla)).append(" ENABLE TRIGGER USER;\n"));
        sql.append("\nCOMMIT;\n");
        return sql.toString();
    }

    /** Identificador entre comillas dobles (los nombres vienen del catálogo de PostgreSQL). */
    static String id(String nombre) {
        return '"' + nombre.replace("\"", "\"\"") + '"';
    }
}
