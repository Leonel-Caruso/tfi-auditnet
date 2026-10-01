package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotorAuditoriaServiceTest {

    private final MotorAuditoriaService motor = new MotorAuditoriaService();

    private static final String CONFIG = """
            hostname RTR-CORE-01
            ip ssh version 2
            logging buffered   16384
            ntp server 10.0.0.1
            ntp server 10.0.0.2
            line vty 0 4
             transport input telnet ssh
            """;

    @Test
    void evaluaPresenciaYAusenciaGenerandoUnHallazgo() {
        List<ReglaBaseline> reglas = List.of(
                regla(1L, "SEC-SSH-01", TipoReglaConfiguracion.DEBE_CONTENER, "ip ssh version 2", null, EstadoRegla.ACTIVA),
                regla(2L, "SEC-TELNET-01", TipoReglaConfiguracion.NO_DEBE_CONTENER, "transport input telnet", null, EstadoRegla.ACTIVA)
        );

        var resultados = motor.evaluar(CONFIG, reglas);

        assertEquals(2, resultados.size());
        assertTrue(resultados.get(0).cumple());
        assertFalse(resultados.get(1).cumple());
        assertTrue(resultados.get(1).evidencia().contains("transport input telnet"));
        assertEquals("Impacto de prueba", resultados.get(1).impacto());
    }

    @Test
    void ignoraReglasInactivas() {
        ReglaBaseline inactiva = regla(3L, "OLD-01", TipoReglaConfiguracion.DEBE_CONTENER, "legacy", null, EstadoRegla.INACTIVA);

        var resultados = motor.evaluar("hostname router", List.of(inactiva));

        assertTrue(resultados.isEmpty());
    }

    @Test
    void valorEsperadoCumpleIgnorandoMayusculasYEspaciosRepetidos() {
        var resultado = motor.evaluar(CONFIG, List.of(
                regla(4L, "LOG-01", TipoReglaConfiguracion.VALOR_ESPERADO, "Logging  Buffered", "16384", EstadoRegla.ACTIVA)
        )).get(0);

        assertTrue(resultado.cumple());
        // La copia de la condición incluye el valor esperado.
        assertEquals("Logging  Buffered = 16384", resultado.patron());
    }

    @Test
    void valorEsperadoCumpleSiAlgunaLineaDelParametroTieneElValor() {
        var resultado = motor.evaluar(CONFIG, List.of(
                regla(5L, "NTP-01", TipoReglaConfiguracion.VALOR_ESPERADO, "ntp server", "10.0.0.2", EstadoRegla.ACTIVA)
        )).get(0);

        assertTrue(resultado.cumple());
    }

    @Test
    void valorEsperadoDistintoGeneraHallazgoConLosValoresEncontrados() {
        var resultado = motor.evaluar(CONFIG, List.of(
                regla(6L, "NTP-02", TipoReglaConfiguracion.VALOR_ESPERADO, "ntp server", "10.9.9.9", EstadoRegla.ACTIVA)
        )).get(0);

        assertFalse(resultado.cumple());
        assertTrue(resultado.evidencia().contains("10.0.0.1, 10.0.0.2"));
        assertTrue(resultado.evidencia().contains("se esperaba: 10.9.9.9"));
    }

    @Test
    void valorEsperadoSinElParametroGeneraHallazgo() {
        var resultado = motor.evaluar(CONFIG, List.of(
                regla(7L, "SNMP-01", TipoReglaConfiguracion.VALOR_ESPERADO, "snmp-server location", "DC1", EstadoRegla.ACTIVA)
        )).get(0);

        assertFalse(resultado.cumple());
        assertTrue(resultado.evidencia().startsWith("No se encontró el parámetro snmp-server location"));
    }

    @Test
    void elParametroDebeCoincidirConPalabrasCompletas() {
        // "ip ssh" no debe tomar "ip ssh version 2" como valor "version 2" de un parámetro "ip ssh versio".
        var resultado = motor.evaluar(CONFIG, List.of(
                regla(8L, "SSH-02", TipoReglaConfiguracion.VALOR_ESPERADO, "ip ssh versio", "n 2", EstadoRegla.ACTIVA)
        )).get(0);

        assertFalse(resultado.cumple());
    }

    private static ReglaBaseline regla(Long id, String codigo, TipoReglaConfiguracion tipo, String patron,
                                       String valorEsperado, EstadoRegla estado) {
        return new ReglaBaseline(id, 1L, codigo, "Regla " + codigo, "desc", tipo, patron, valorEsperado,
                SeveridadRegla.ALTA, "Impacto de prueba", "Corregir", estado);
    }
}
