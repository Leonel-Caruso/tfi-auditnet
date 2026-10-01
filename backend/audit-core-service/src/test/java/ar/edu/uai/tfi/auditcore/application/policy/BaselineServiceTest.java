package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineServiceTest {

    private final RepositoriosEnMemoria.Baselines baselines = new RepositoriosEnMemoria.Baselines();
    private final RepositoriosEnMemoria.Reglas reglas = new RepositoriosEnMemoria.Reglas();
    private final RepositoriosEnMemoria.Auditorias auditorias = new RepositoriosEnMemoria.Auditorias();
    private final List<Transaccion> transacciones = new ArrayList<>();
    private final BaselineService service = new BaselineService(baselines, reglas, auditorias, transacciones::add);

    @Test
    void creaBaselineActivaEnVersionUno() {
        BaselineConfiguracion creada = service.crear(
                " Hardening Cisco IOS ",
                "Configuración mínima esperada",
                1L,
                2L,
                "admin"
        );

        assertEquals("Hardening Cisco IOS", creada.nombre());
        assertEquals(1, creada.version());
        assertEquals(EstadoBaseline.ACTIVO, creada.estado());
        assertEquals(2L, creada.organizacionId());
        assertEquals(OperacionTransaccion.ALTA, transacciones.get(0).operacion());
    }

    @Test
    void rechazaNombreDuplicadoDentroDeLaMismaOrganizacion() {
        baselines.items.add(baseline(1L, "Hardening Cisco IOS", 1, 1L, EstadoBaseline.INACTIVO));

        assertThrows(IllegalStateException.class, () -> service.crear(
                "hardening cisco ios", "Otra", 1L, 2L, "admin"
        ));
    }

    @Test
    void rechazaUnaSegundaBaselineActivaParaElMismoAlcance() {
        baselines.items.add(baseline(1L, "Hardening Cisco IOS", 1, 1L, EstadoBaseline.ACTIVO));

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                service.crear("Otra baseline", "desc", 1L, 2L, "admin"));
        assertTrue(error.getMessage().contains("Hardening Cisco IOS v1"));

        // Otro tipo de dispositivo es otro alcance: se permite.
        assertEquals(EstadoBaseline.ACTIVO, service.crear("Switches", "desc", 9L, 2L, "admin").estado());
    }

    @Test
    void nuevaVersionCopiaReglasActivasYReemplazaALaVigente() {
        baselines.items.add(baseline(1L, "Hardening Cisco IOS", 1, 1L, EstadoBaseline.ACTIVO));
        reglas.items.add(regla(10L, 1L, "SEC-SSH-01", EstadoRegla.ACTIVA));
        reglas.items.add(regla(11L, 1L, "OLD-01", EstadoRegla.INACTIVA));
        auditorias.porBaseline.put(1L, 3L);

        BaselineConfiguracion v2 = service.nuevaVersion(1L, "Agrega control de logging", "admin");

        assertEquals(2, v2.version());
        assertEquals("Hardening Cisco IOS", v2.nombre());
        assertEquals("Agrega control de logging", v2.descripcion());
        assertEquals(EstadoBaseline.ACTIVO, v2.estado());
        assertEquals(EstadoBaseline.INACTIVO, baselines.buscarPorId(1L).orElseThrow().estado());

        List<ReglaBaseline> copiadas = reglas.listarPorBaseline(v2.id());
        assertEquals(1, copiadas.size());
        assertEquals("SEC-SSH-01", copiadas.get(0).codigo());
        assertEquals("Impacto de prueba", copiadas.get(0).impacto());
        // La versión anterior conserva sus reglas.
        assertEquals(2, reglas.listarPorBaseline(1L).size());

        assertEquals(List.of(OperacionTransaccion.CAMBIO_ESTADO, OperacionTransaccion.ALTA, OperacionTransaccion.ALTA),
                transacciones.stream().map(Transaccion::operacion).toList());
        assertEquals("BASELINE", transacciones.get(2).entidad());
        assertTrue(transacciones.get(2).detalle().contains("reglas copiadas: 1"));
    }

    @Test
    void laVersionNuevaSeNumeraDespuesDeLaUltimaAunqueSeGenereDesdeUnaAnterior() {
        baselines.items.add(baseline(1L, "Hardening", 1, 1L, EstadoBaseline.INACTIVO));
        baselines.items.add(baseline(2L, "Hardening", 2, 1L, EstadoBaseline.ACTIVO));

        BaselineConfiguracion v3 = service.nuevaVersion(1L, null, "admin");

        assertEquals(3, v3.version());
        assertEquals("Base", v3.descripcion());
        assertEquals(EstadoBaseline.INACTIVO, baselines.buscarPorId(2L).orElseThrow().estado());
    }

    @Test
    void activarRequiereQueNoHayaOtraActivaEnElAlcance() {
        baselines.items.add(baseline(1L, "Hardening", 1, 1L, EstadoBaseline.INACTIVO));
        baselines.items.add(baseline(2L, "Hardening", 2, 1L, EstadoBaseline.ACTIVO));

        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(1L, "ACTIVO", "admin"));

        service.cambiarEstado(2L, "inactivo", "admin");
        BaselineConfiguracion reactivada = service.cambiarEstado(1L, "ACTIVO", "admin");

        assertEquals(EstadoBaseline.ACTIVO, reactivada.estado());
        assertEquals(OperacionTransaccion.CAMBIO_ESTADO, transacciones.get(1).operacion());
        assertEquals(baseline(1L, "Hardening", 1, 1L, EstadoBaseline.INACTIVO), transacciones.get(1).valorAnterior());
    }

    @Test
    void modificarDescripcionRegistraModificacion() {
        baselines.items.add(baseline(1L, "Hardening", 1, 1L, EstadoBaseline.ACTIVO));

        BaselineConfiguracion modificada = service.modificarDescripcion(1L, " Nueva descripción ", "admin");

        assertEquals("Nueva descripción", modificada.descripcion());
        assertEquals(1, modificada.version());
        assertEquals(OperacionTransaccion.MODIFICACION, transacciones.get(0).operacion());
        assertThrows(IllegalArgumentException.class, () -> service.modificarDescripcion(1L, " ", "admin"));
    }

    private static BaselineConfiguracion baseline(Long id, String nombre, int version, Long tipo, EstadoBaseline estado) {
        return new BaselineConfiguracion(id, nombre, "Base", version, tipo, 2L, estado);
    }

    private static ReglaBaseline regla(Long id, Long baselineId, String codigo, EstadoRegla estado) {
        return new ReglaBaseline(id, baselineId, codigo, "Regla " + codigo, "desc",
                TipoReglaConfiguracion.DEBE_CONTENER, "patron " + codigo, null, SeveridadRegla.ALTA,
                "Impacto de prueba", "recom", estado);
    }
}
