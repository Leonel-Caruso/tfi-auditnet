package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotorAuditoriaServiceTest {

    private final MotorAuditoriaService motor = new MotorAuditoriaService();

    @Test
    void evaluaPresenciaYAusenciaGenerandoUnHallazgo() {
        List<ReglaBaseline> reglas = List.of(
                new ReglaBaseline(
                        1L, 1L, "SEC-SSH-01", "Forzar SSH version 2", "desc",
                        TipoReglaConfiguracion.DEBE_CONTENER, "ip ssh version 2",
                        SeveridadRegla.ALTA, "Configurar SSH v2", EstadoRegla.ACTIVA
                ),
                new ReglaBaseline(
                        2L, 1L, "SEC-TELNET-01", "Bloquear Telnet", "desc",
                        TipoReglaConfiguracion.NO_DEBE_CONTENER, "transport input telnet",
                        SeveridadRegla.ALTA, "Usar SSH", EstadoRegla.ACTIVA
                )
        );

        String config = """
                hostname RTR-CORE-01
                ip ssh version 2
                line vty 0 4
                 transport input telnet ssh
                """;

        var resultados = motor.evaluar(config, reglas);

        assertEquals(2, resultados.size());
        assertTrue(resultados.get(0).cumple());
        assertFalse(resultados.get(1).cumple());
        assertTrue(resultados.get(1).evidencia().contains("transport input telnet"));
    }

    @Test
    void ignoraReglasInactivas() {
        ReglaBaseline inactiva = new ReglaBaseline(
                3L, 1L, "OLD-01", "Regla vieja", "desc",
                TipoReglaConfiguracion.DEBE_CONTENER, "legacy",
                SeveridadRegla.BAJA, "n/a", EstadoRegla.INACTIVA
        );

        var resultados = motor.evaluar("hostname router", List.of(inactiva));

        assertTrue(resultados.isEmpty());
    }
}
