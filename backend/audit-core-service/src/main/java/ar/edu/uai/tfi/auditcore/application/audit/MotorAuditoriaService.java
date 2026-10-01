package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Motor de comparación: evalúa cada regla activa contra la configuración normalizada.
 * La comparación no distingue mayúsculas y, para VALOR_ESPERADO, ignora espacios repetidos.
 */
@ApplicationScoped
public class MotorAuditoriaService {

    private static final int MAX_VALORES_EN_EVIDENCIA = 3;

    public List<EvaluacionCalculada> evaluar(String configuracionNormalizada, List<ReglaBaseline> reglas) {
        if (configuracionNormalizada == null || configuracionNormalizada.isBlank()) {
            throw new IllegalArgumentException("La configuración normalizada es obligatoria para ejecutar la auditoría.");
        }
        if (reglas == null || reglas.isEmpty()) {
            throw new IllegalArgumentException("La auditoría requiere al menos una regla activa.");
        }

        return reglas.stream()
                .filter(regla -> regla.estado() == EstadoRegla.ACTIVA)
                .map(regla -> evaluarRegla(configuracionNormalizada, regla))
                .toList();
    }

    private EvaluacionCalculada evaluarRegla(String configuracion, ReglaBaseline regla) {
        String patron = regla.patron().trim();
        Resultado resultado = regla.tipo() == TipoReglaConfiguracion.VALOR_ESPERADO
                ? evaluarValorEsperado(configuracion, patron, regla.valorEsperado())
                : evaluarPresencia(configuracion, regla.tipo(), patron);

        // La copia que queda en la evaluación y el hallazgo incluye el valor esperado, para reconstruir la condición.
        String condicion = regla.tipo() == TipoReglaConfiguracion.VALOR_ESPERADO
                ? patron + " = " + regla.valorEsperado()
                : patron;

        return new EvaluacionCalculada(
                regla.id(),
                regla.codigo(),
                regla.nombre(),
                regla.tipo(),
                recortar(condicion, 500),
                regla.severidad(),
                resultado.cumple(),
                recortar(resultado.evidencia(), 800),
                regla.recomendacion(),
                regla.impacto()
        );
    }

    private Resultado evaluarPresencia(String configuracion, TipoReglaConfiguracion tipo, String patron) {
        String patronComparacion = patron.toLowerCase(Locale.ROOT);
        boolean contiene = configuracion.toLowerCase(Locale.ROOT).contains(patronComparacion);
        boolean cumple = tipo == TipoReglaConfiguracion.DEBE_CONTENER ? contiene : !contiene;

        if (contiene) {
            String linea = primeraLineaConPatron(configuracion, patronComparacion, patron);
            return new Resultado(cumple, tipo == TipoReglaConfiguracion.DEBE_CONTENER
                    ? "Patrón requerido encontrado: " + linea
                    : "Patrón no permitido detectado: " + linea);
        }
        return new Resultado(cumple, tipo == TipoReglaConfiguracion.DEBE_CONTENER
                ? "No se encontró el patrón requerido: " + patron
                : "No se detectó el patrón no permitido: " + patron);
    }

    /**
     * Busca las líneas que empiezan con el parámetro (seguido de un espacio o fin de línea) y toma
     * el resto de la línea como valor. Cumple si alguna tiene exactamente el valor esperado.
     */
    private Resultado evaluarValorEsperado(String configuracion, String parametro, String valorEsperado) {
        String parametroComparacion = compactar(parametro).toLowerCase(Locale.ROOT);
        String esperado = compactar(valorEsperado == null ? "" : valorEsperado);

        List<String> valores = configuracion.lines()
                .map(MotorAuditoriaService::compactar)
                .filter(linea -> {
                    String minuscula = linea.toLowerCase(Locale.ROOT);
                    return minuscula.equals(parametroComparacion) || minuscula.startsWith(parametroComparacion + " ");
                })
                .map(linea -> linea.substring(parametroComparacion.length()).trim())
                .toList();

        if (valores.isEmpty()) {
            return new Resultado(false, "No se encontró el parámetro " + parametro
                    + " (valor esperado: " + esperado + ").");
        }

        boolean cumple = valores.stream().anyMatch(valor -> valor.equalsIgnoreCase(esperado));
        if (cumple) {
            return new Resultado(true, "Parámetro " + parametro + " con el valor esperado: " + esperado + ".");
        }

        String encontrados = valores.stream()
                .limit(MAX_VALORES_EN_EVIDENCIA)
                .map(valor -> valor.isEmpty() ? "(sin valor)" : valor)
                .collect(Collectors.joining(", "));
        String resto = valores.size() > MAX_VALORES_EN_EVIDENCIA
                ? " y " + (valores.size() - MAX_VALORES_EN_EVIDENCIA) + " más"
                : "";
        return new Resultado(false, "Parámetro " + parametro + " con valor " + encontrados + resto
                + "; se esperaba: " + esperado + ".");
    }

    private String primeraLineaConPatron(String configuracion, String patronComparacion, String patron) {
        return configuracion.lines()
                .map(String::trim)
                .filter(linea -> linea.toLowerCase(Locale.ROOT).contains(patronComparacion))
                .findFirst()
                .orElse(patron);
    }

    /** Quita espacios al inicio y al final y reduce los espacios internos a uno solo. */
    private static String compactar(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo - 3) + "...";
    }

    private record Resultado(boolean cumple, String evidencia) {
    }

    public record EvaluacionCalculada(
            Long reglaId,
            String codigoRegla,
            String nombreRegla,
            TipoReglaConfiguracion tipo,
            String patron,
            SeveridadRegla severidad,
            boolean cumple,
            String evidencia,
            String recomendacion,
            String impacto
    ) {
    }
}
