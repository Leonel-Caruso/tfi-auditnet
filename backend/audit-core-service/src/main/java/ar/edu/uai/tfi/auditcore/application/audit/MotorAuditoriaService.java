package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MotorAuditoriaService {

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
        String configuracionComparacion = configuracion.toLowerCase(Locale.ROOT);
        String patronComparacion = patron.toLowerCase(Locale.ROOT);

        boolean contiene = configuracionComparacion.contains(patronComparacion);
        boolean cumple = switch (regla.tipo()) {
            case DEBE_CONTENER -> contiene;
            case NO_DEBE_CONTENER -> !contiene;
        };

        String evidencia = construirEvidencia(configuracion, regla.tipo(), patron, contiene, cumple);

        return new EvaluacionCalculada(
                regla.id(),
                regla.codigo(),
                regla.nombre(),
                regla.tipo(),
                patron,
                regla.severidad(),
                cumple,
                evidencia,
                regla.recomendacion()
        );
    }

    private String construirEvidencia(
            String configuracion,
            TipoReglaConfiguracion tipo,
            String patron,
            boolean contiene,
            boolean cumple
    ) {
        if (contiene) {
            String linea = primeraLineaConPatron(configuracion, patron);
            if (tipo == TipoReglaConfiguracion.DEBE_CONTENER) {
                return "Patrón requerido encontrado: " + linea;
            }
            return "Patrón no permitido detectado: " + linea;
        }

        if (tipo == TipoReglaConfiguracion.DEBE_CONTENER) {
            return "No se encontró el patrón requerido: " + patron;
        }

        return cumple
                ? "No se detectó el patrón no permitido: " + patron
                : "El patrón no permitido no pudo evaluarse.";
    }

    private String primeraLineaConPatron(String configuracion, String patron) {
        String patronComparacion = patron.toLowerCase(Locale.ROOT);
        return configuracion.lines()
                .map(String::trim)
                .filter(linea -> linea.toLowerCase(Locale.ROOT).contains(patronComparacion))
                .findFirst()
                .orElse(patron);
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
            String recomendacion
    ) {
    }
}
