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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglaBaselineServiceTest {

    private final RepositoriosEnMemoria.Baselines baselines = new RepositoriosEnMemoria.Baselines();
    private final RepositoriosEnMemoria.Reglas reglas = new RepositoriosEnMemoria.Reglas();
    private final RepositoriosEnMemoria.Auditorias auditorias = new RepositoriosEnMemoria.Auditorias();
    private final List<Transaccion> transacciones = new ArrayList<>();
    private final ReglaBaselineService service =
            new ReglaBaselineService(reglas, baselines, auditorias, transacciones::add);

    ReglaBaselineServiceTest() {
        baselines.items.add(new BaselineConfiguracion(1L, "Hardening Cisco IOS", "Base", 1, 1L, 2L, EstadoBaseline.ACTIVO));
    }

    @Test
    void creaReglaNormalizandoCodigoYClasificacion() {
        ReglaBaseline creada = service.crear(1L, " sec-ssh-01 ", datos("debe_contener", "ip ssh version 2", null), "admin");

        assertEquals("SEC-SSH-01", creada.codigo());
        assertEquals(TipoReglaConfiguracion.DEBE_CONTENER, creada.tipo());
        assertEquals(SeveridadRegla.ALTA, creada.severidad());
        assertEquals(EstadoRegla.ACTIVA, creada.estado());
        assertEquals("Acceso administrativo sin cifrar", creada.impacto());
        assertNull(creada.valorEsperado());
        assertEquals(OperacionTransaccion.ALTA, transacciones.get(0).operacion());
        assertEquals(2L, transacciones.get(0).organizacionId());
    }

    @Test
    void rechazaCodigoDuplicadoDentroDeLaMismaBaseline() {
        service.crear(1L, "SEC-SSH-01", datos("DEBE_CONTENER", "ip ssh version 2", null), "admin");

        assertThrows(IllegalStateException.class, () ->
                service.crear(1L, "sec-ssh-01", datos("DEBE_CONTENER", "otra cosa", null), "admin"));
    }

    @Test
    void reglaValorEsperadoRequiereElValorYLosDemasTiposNoLoAdmiten() {
        ReglaBaseline creada = service.crear(1L, "LOG-01", datos("VALOR_ESPERADO", "logging buffered", " 16384 "), "admin");
        assertEquals("16384", creada.valorEsperado());

        assertThrows(IllegalArgumentException.class, () ->
                service.crear(1L, "LOG-02", datos("VALOR_ESPERADO", "logging host", null), "admin"));
        assertThrows(IllegalArgumentException.class, () ->
                service.crear(1L, "SEC-02", datos("DEBE_CONTENER", "service password-encryption", "x"), "admin"));
    }

    @Test
    void rechazaCondicionesQueElMotorNoPuedeInterpretar() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crear(1L, "SEC-03", datos("DEBE_CONTENER", "linea 1\nlinea 2", null), "admin"));
        assertThrows(IllegalArgumentException.class, () ->
                service.crear(1L, "SEC-04", datos("CONTIENE_ALGO", "x", null), "admin"));
        assertThrows(IllegalArgumentException.class, () ->
                service.crear(1L, "con espacios", datos("DEBE_CONTENER", "x", null), "admin"));
        ReglaBaselineService.DatosRegla sinImpacto = new ReglaBaselineService.DatosRegla(
                "SSH", "desc", "DEBE_CONTENER", "ip ssh version 2", null, "ALTA", " ", "recom");
        assertThrows(IllegalArgumentException.class, () -> service.crear(1L, "SEC-05", sinImpacto, "admin"));
    }

    @Test
    void rechazaDosReglasActivasConLaMismaCondicion() {
        service.crear(1L, "SEC-SSH-01", datos("DEBE_CONTENER", "ip ssh version 2", null), "admin");

        assertThrows(IllegalStateException.class, () ->
                service.crear(1L, "SEC-SSH-02", datos("DEBE_CONTENER", "IP SSH VERSION 2", null), "admin"));
    }

    @Test
    void modificarRegistraValorAnteriorYNuevo() {
        ReglaBaseline creada = service.crear(1L, "SEC-SSH-01", datos("DEBE_CONTENER", "ip ssh version 2", null), "admin");

        ReglaBaseline modificada = service.modificar(creada.id(),
                datos("VALOR_ESPERADO", "ip ssh version", "2"), "admin");

        assertEquals("SEC-SSH-01", modificada.codigo());
        assertEquals(TipoReglaConfiguracion.VALOR_ESPERADO, modificada.tipo());
        Transaccion modificacion = transacciones.get(1);
        assertEquals(OperacionTransaccion.MODIFICACION, modificacion.operacion());
        assertEquals(creada, modificacion.valorAnterior());
        assertEquals(modificada, modificacion.valorNuevo());
    }

    @Test
    void noCreaNiModificaReglasDeUnaBaselineYaAuditada() {
        ReglaBaseline creada = service.crear(1L, "SEC-SSH-01", datos("DEBE_CONTENER", "ip ssh version 2", null), "admin");
        auditorias.porBaseline.put(1L, 2L);

        IllegalStateException alCrear = assertThrows(IllegalStateException.class, () ->
                service.crear(1L, "SEC-NEW", datos("DEBE_CONTENER", "aaa new-model", null), "admin"));
        assertTrue(alCrear.getMessage().contains("nueva versión"));
        assertThrows(IllegalStateException.class, () ->
                service.modificar(creada.id(), datos("DEBE_CONTENER", "otro", null), "admin"));

        // La baja lógica sí se permite: no sobrescribe la regla.
        ReglaBaseline inactiva = service.cambiarEstado(creada.id(), "INACTIVA", "admin");
        assertEquals(EstadoRegla.INACTIVA, inactiva.estado());
        assertEquals(OperacionTransaccion.BAJA_LOGICA, transacciones.get(transacciones.size() - 1).operacion());
        // Reactivarla agregaría un criterio a una baseline ya auditada.
        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(creada.id(), "ACTIVA", "admin"));
    }

    @Test
    void rechazaReglasContradictoriasSobreElMismoPatron() {
        service.crear(1L, "SEC-TEL-01", datos("NO_DEBE_CONTENER", "transport input telnet", null), "admin");

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                service.crear(1L, "SEC-TEL-02", datos("DEBE_CONTENER", "transport input telnet", null), "admin"));
        assertTrue(error.getMessage().contains("lo contrario"));
    }

    @Test
    void noCreaReglasEnUnaBaselineInactiva() {
        baselines.items.add(new BaselineConfiguracion(2L, "Vieja", "Base", 1, 1L, 2L, EstadoBaseline.INACTIVO));

        assertThrows(IllegalStateException.class, () ->
                service.crear(2L, "SEC-01", datos("DEBE_CONTENER", "x", null), "admin"));
    }

    private static ReglaBaselineService.DatosRegla datos(String tipo, String patron, String valorEsperado) {
        return new ReglaBaselineService.DatosRegla(
                "SSH versión 2",
                "La configuración debe forzar SSH v2",
                tipo,
                patron,
                valorEsperado,
                "alta",
                "Acceso administrativo sin cifrar",
                "Configurar ip ssh version 2"
        );
    }
}
