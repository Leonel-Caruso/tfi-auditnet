package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.audit.ComparadorAuditorias.CambioRegla;
import ar.edu.uai.tfi.auditcore.application.audit.ComparadorAuditorias.Categoria;
import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComparadorAuditoriasTest {

    @Test
    void clasificaCadaReglaSegunLaEvolucionEntreAuditorias() {
        List<ResultadoReglaAuditoria> anterior = List.of(
                evaluacion("SSH", SeveridadRegla.ALTA, false),     // se corrige
                evaluacion("TELNET", SeveridadRegla.ALTA, true),   // aparece un desvío
                evaluacion("NTP", SeveridadRegla.BAJA, false),     // persiste
                evaluacion("BANNER", SeveridadRegla.BAJA, true),   // sigue cumpliendo
                evaluacion("VIEJA", SeveridadRegla.MEDIA, true)    // ya no se evalúa
        );
        List<ResultadoReglaAuditoria> actual = List.of(
                evaluacion("SSH", SeveridadRegla.ALTA, true),
                evaluacion("TELNET", SeveridadRegla.ALTA, false),
                evaluacion("NTP", SeveridadRegla.BAJA, false),
                evaluacion("BANNER", SeveridadRegla.BAJA, true),
                evaluacion("LOG", SeveridadRegla.CRITICA, false),  // regla nueva con desvío
                evaluacion("AAA", SeveridadRegla.MEDIA, true)      // regla nueva que cumple
        );

        List<CambioRegla> cambios = ComparadorAuditorias.comparar(anterior, actual);

        // Orden: por categoría y, dentro de cada una, por severidad descendente.
        assertEquals(List.of("LOG", "TELNET", "SSH", "NTP", "BANNER", "AAA", "VIEJA"),
                cambios.stream().map(CambioRegla::codigoRegla).toList());
        assertEquals(Categoria.NUEVO_HALLAZGO, cambios.get(0).categoria());
        assertNull(cambios.get(0).cumpleAntes());
        assertEquals(Categoria.CORREGIDO, cambios.get(2).categoria());
        assertEquals(Categoria.REGLA_RETIRADA, cambios.get(6).categoria());
        assertNull(cambios.get(6).cumpleAhora());

        Map<Categoria, Long> resumen = ComparadorAuditorias.resumen(cambios);
        assertEquals(2L, resumen.get(Categoria.NUEVO_HALLAZGO));
        assertEquals(1L, resumen.get(Categoria.CORREGIDO));
        assertEquals(1L, resumen.get(Categoria.PERSISTENTE));
        assertEquals(1L, resumen.get(Categoria.SIN_CAMBIO));
        assertEquals(1L, resumen.get(Categoria.REGLA_NUEVA));
        assertEquals(1L, resumen.get(Categoria.REGLA_RETIRADA));
    }

    @Test
    void laPrimeraAuditoriaDeUnDispositivoNoTieneAnterior() {
        List<CambioRegla> cambios = ComparadorAuditorias.comparar(List.of(),
                List.of(evaluacion("SSH", SeveridadRegla.ALTA, false), evaluacion("NTP", SeveridadRegla.BAJA, true)));

        assertEquals(Categoria.NUEVO_HALLAZGO, cambios.get(0).categoria());
        assertEquals(Categoria.REGLA_NUEVA, cambios.get(1).categoria());
        assertEquals(0L, ComparadorAuditorias.resumen(cambios).get(Categoria.CORREGIDO));
    }

    private static ResultadoReglaAuditoria evaluacion(String codigo, SeveridadRegla severidad, boolean cumple) {
        return new ResultadoReglaAuditoria(null, 1L, 1L, codigo, "Regla " + codigo,
                TipoReglaConfiguracion.DEBE_CONTENER, "patron", severidad, cumple, "evidencia");
    }
}
