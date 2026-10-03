package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compara las evaluaciones de dos auditorías del mismo dispositivo regla por regla (por código), para
 * ver la evolución en el tiempo: qué desvíos aparecieron, cuáles se corrigieron y cuáles persisten.
 * Trabaja sobre las copias guardadas en cada auditoría, así que no depende de cambios posteriores en
 * las reglas o la baseline.
 */
public final class ComparadorAuditorias {

    public enum Categoria {
        /** Antes cumplía (o no se evaluaba) y ahora no cumple. */
        NUEVO_HALLAZGO,
        /** Antes no cumplía y ahora cumple. */
        CORREGIDO,
        /** No cumplía en ninguna de las dos. */
        PERSISTENTE,
        /** Cumplía en las dos. */
        SIN_CAMBIO,
        /** Solo se evaluó en la auditoría actual y cumple. */
        REGLA_NUEVA,
        /** Solo se evaluó en la auditoría anterior. */
        REGLA_RETIRADA
    }

    public record CambioRegla(
            String codigoRegla,
            String nombreRegla,
            SeveridadRegla severidad,
            Boolean cumpleAntes,
            Boolean cumpleAhora,
            Categoria categoria
    ) {
    }

    private ComparadorAuditorias() {
    }

    public static List<CambioRegla> comparar(List<ResultadoReglaAuditoria> anteriores,
                                             List<ResultadoReglaAuditoria> actuales) {
        Map<String, ResultadoReglaAuditoria> antes = porCodigo(anteriores);
        Map<String, ResultadoReglaAuditoria> ahora = porCodigo(actuales);
        List<CambioRegla> cambios = new ArrayList<>();

        for (ResultadoReglaAuditoria actual : ahora.values()) {
            ResultadoReglaAuditoria previa = antes.get(actual.codigoRegla());
            Boolean cumpleAntes = previa == null ? null : previa.cumple();
            cambios.add(new CambioRegla(actual.codigoRegla(), actual.nombreRegla(), actual.severidad(),
                    cumpleAntes, actual.cumple(), categoria(cumpleAntes, actual.cumple())));
        }
        for (ResultadoReglaAuditoria previa : antes.values()) {
            if (!ahora.containsKey(previa.codigoRegla())) {
                cambios.add(new CambioRegla(previa.codigoRegla(), previa.nombreRegla(), previa.severidad(),
                        previa.cumple(), null, Categoria.REGLA_RETIRADA));
            }
        }

        cambios.sort(Comparator
                .comparing((CambioRegla cambio) -> cambio.categoria().ordinal())
                .thenComparing(cambio -> -rango(cambio.severidad()))
                .thenComparing(CambioRegla::codigoRegla));
        return cambios;
    }

    /** Cantidad de reglas por categoría (incluye las categorías en cero). */
    public static Map<Categoria, Long> resumen(List<CambioRegla> cambios) {
        Map<Categoria, Long> resumen = new EnumMap<>(Categoria.class);
        for (Categoria categoria : Categoria.values()) {
            resumen.put(categoria, 0L);
        }
        cambios.forEach(cambio -> resumen.merge(cambio.categoria(), 1L, Long::sum));
        return resumen;
    }

    private static Categoria categoria(Boolean cumpleAntes, boolean cumpleAhora) {
        if (cumpleAntes == null) {
            return cumpleAhora ? Categoria.REGLA_NUEVA : Categoria.NUEVO_HALLAZGO;
        }
        if (cumpleAntes) {
            return cumpleAhora ? Categoria.SIN_CAMBIO : Categoria.NUEVO_HALLAZGO;
        }
        return cumpleAhora ? Categoria.CORREGIDO : Categoria.PERSISTENTE;
    }

    private static Map<String, ResultadoReglaAuditoria> porCodigo(List<ResultadoReglaAuditoria> evaluaciones) {
        Map<String, ResultadoReglaAuditoria> mapa = new LinkedHashMap<>();
        evaluaciones.forEach(evaluacion -> mapa.putIfAbsent(evaluacion.codigoRegla(), evaluacion));
        return mapa;
    }

    private static int rango(SeveridadRegla severidad) {
        return severidad == null ? 0 : severidad.ordinal() + 1;
    }
}
