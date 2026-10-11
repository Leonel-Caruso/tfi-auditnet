package ar.edu.uai.tfi.auditcore.infrastructure.backup;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Bloque 7 · Orden de restauración por claves foráneas y guion de restauración. */
class GuionRestauracionTest {

    private static EstructuraBase estructura() {
        Map<String, List<String>> columnas = new LinkedHashMap<>();
        columnas.put("usuarios", List.of("id_usuario", "id_organizacion"));
        columnas.put("organizaciones_cliente", List.of("id_organizacion", "razon_social"));
        columnas.put("sedes", List.of("id_sede", "id_organizacion"));
        columnas.put("dispositivos_red", List.of("id_dispositivo", "id_organizacion", "id_sede"));
        columnas.put("flyway_historial_management", List.of("installed_rank", "version"));
        Map<String, Set<String>> dependencias = new LinkedHashMap<>();
        dependencias.put("usuarios", Set.of("organizaciones_cliente"));
        dependencias.put("sedes", Set.of("organizaciones_cliente"));
        dependencias.put("dispositivos_red", Set.of("organizaciones_cliente", "sedes", "dispositivos_red"));
        Map<String, List<String>> secuencias = Map.of("usuarios", List.of("id_usuario"), "sedes", List.of("id_sede"));
        return new EstructuraBase(columnas, secuencias, dependencias);
    }

    @Test
    void ordenaPrimeroLasTablasReferenciadasYExcluyeFlyway() {
        List<String> orden = estructura().ordenRestauracion();

        assertEquals(List.of("organizaciones_cliente", "sedes", "usuarios", "dispositivos_red"), orden);
        assertEquals(List.of("flyway_historial_management"), estructura().historialesFlyway());
    }

    @Test
    void elGuionVerificaLaVersionVaciaCargaYAjustaSecuenciasEnUnaTransaccion() {
        String guion = GuionRestauracion.generar("auditnet-respaldo-x.zip", estructura(),
                Map.of("flyway_historial_management", "4"));

        assertTrue(guion.contains("\\set ON_ERROR_STOP on"));
        assertTrue(guion.indexOf("BEGIN;") < guion.indexOf("TRUNCATE"));
        assertTrue(guion.contains("IF v IS DISTINCT FROM '4' THEN"));
        assertTrue(guion.contains("ALTER TABLE \"usuarios\" DISABLE TRIGGER USER;"));
        assertTrue(guion.contains("TRUNCATE TABLE \"organizaciones_cliente\", \"sedes\", \"usuarios\", \"dispositivos_red\" RESTART IDENTITY CASCADE;"));
        assertTrue(guion.indexOf("\\copy \"organizaciones_cliente\"") < guion.indexOf("\\copy \"dispositivos_red\""));
        assertTrue(guion.contains("\\copy \"usuarios\" (\"id_usuario\", \"id_organizacion\") FROM 'datos/usuarios.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')"));
        assertTrue(guion.contains("SELECT setval(pg_get_serial_sequence('\"usuarios\"', 'id_usuario'), coalesce(max(\"id_usuario\"), 0) + 1, false) FROM \"usuarios\";"));
        assertFalse(guion.contains("\\copy \"flyway_historial_management\""), "el historial de Flyway no se pisa");
        assertTrue(guion.indexOf("ENABLE TRIGGER USER") < guion.lastIndexOf("COMMIT;"));
    }
}
