package ar.edu.uai.tfi.auditcore.application.admin;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.List;

/**
 * Salud del servicio para el panel de administración: versión, tiempo en línea, memoria y estado de cada componente.
 * Estado general UP si todo responde, DEGRADADO si el servicio responde pero falla algún componente.
 */
@ApplicationScoped
public class SaludService {

    private final SaludPort componentes;

    @ConfigProperty(name = "app.name")
    String servicio;

    @ConfigProperty(name = "app.version", defaultValue = "local")
    String version;

    public SaludService(SaludPort componentes) {
        this.componentes = componentes;
    }

    public record SaludServicio(String servicio, String estado, String version, Instant inicio, long segundosEnLinea,
                                long memoriaUsadaMb, long memoriaMaximaMb, List<ComponenteSalud> componentes,
                                Instant verificado) {
    }

    public SaludServicio verificar() {
        List<ComponenteSalud> resultado = componentes.verificarComponentes();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Runtime memoria = Runtime.getRuntime();
        long usadaMb = (memoria.totalMemory() - memoria.freeMemory()) / (1024 * 1024);
        return new SaludServicio(
                servicio,
                estadoGeneral(resultado),
                version,
                Instant.ofEpochMilli(runtime.getStartTime()),
                runtime.getUptime() / 1000,
                usadaMb,
                memoria.maxMemory() / (1024 * 1024),
                resultado,
                Instant.now());
    }

    static String estadoGeneral(List<ComponenteSalud> componentes) {
        return componentes.stream().allMatch(ComponenteSalud::disponible) ? "UP" : "DEGRADADO";
    }
}
