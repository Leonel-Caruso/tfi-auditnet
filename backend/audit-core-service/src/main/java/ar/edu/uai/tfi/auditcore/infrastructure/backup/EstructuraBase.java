package ar.edu.uai.tfi.auditcore.infrastructure.backup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Estructura de la base que necesita el respaldo: tablas con sus columnas, columnas con secuencia (identity) y
 * dependencias por claves foráneas. Ordena las tablas para restaurarlas (primero las referenciadas).
 *
 * @param columnas      tabla → columnas en el orden de la tabla (sin columnas generadas)
 * @param secuencias    tabla → columnas identity o serial
 * @param dependencias  tabla → tablas que referencia
 */
public record EstructuraBase(Map<String, List<String>> columnas,
                             Map<String, List<String>> secuencias,
                             Map<String, Set<String>> dependencias) {

    /** Tablas de datos (sin los historiales de Flyway) en orden de restauración. */
    public List<String> ordenRestauracion() {
        Set<String> tablas = new TreeSet<>();
        columnas.keySet().stream().filter(tabla -> !esHistorialFlyway(tabla)).forEach(tablas::add);
        Map<String, Set<String>> pendientes = new LinkedHashMap<>();
        for (String tabla : tablas) {
            Set<String> padres = new TreeSet<>(dependencias.getOrDefault(tabla, Set.of()));
            padres.remove(tabla); // autorreferencias
            padres.retainAll(tablas);
            pendientes.put(tabla, padres);
        }
        List<String> orden = new ArrayList<>();
        while (!pendientes.isEmpty()) {
            List<String> listas = pendientes.entrySet().stream()
                    .filter(entrada -> entrada.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .toList();
            if (listas.isEmpty()) {
                // Ciclo de claves foráneas (no hay en AuditNet): se agregan en orden alfabético.
                orden.addAll(pendientes.keySet());
                break;
            }
            orden.addAll(listas);
            listas.forEach(pendientes::remove);
            pendientes.values().forEach(padres -> padres.removeAll(listas));
        }
        return orden;
    }

    public List<String> historialesFlyway() {
        return columnas.keySet().stream().filter(EstructuraBase::esHistorialFlyway).sorted().toList();
    }

    static boolean esHistorialFlyway(String tabla) {
        return tabla.startsWith("flyway_");
    }

    /** Agrega una dependencia (tabla hija → tabla padre). */
    static void agregar(Map<String, Set<String>> dependencias, String hija, String padre) {
        dependencias.computeIfAbsent(hija, clave -> new LinkedHashSet<>()).add(padre);
    }
}
