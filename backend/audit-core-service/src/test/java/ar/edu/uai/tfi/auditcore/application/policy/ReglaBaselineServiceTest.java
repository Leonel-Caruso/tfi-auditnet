package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglaBaselineServiceTest {

    @Test
    void creaReglaNormalizandoCodigoYClasificacion() {
        FakeBaselineRepository baselines = new FakeBaselineRepository();
        FakeReglaRepository reglas = new FakeReglaRepository();
        ReglaBaselineService service = new ReglaBaselineService(reglas, baselines, (actor, accion, detalle) -> {});

        ReglaBaseline creada = service.crear(
                1L,
                " sec-ssh-01 ",
                "SSH versión 2",
                "La configuración debe forzar SSH v2",
                "debe_contener",
                "ip ssh version 2",
                "alta",
                "Configurar ip ssh version 2",
                "admin"
        );

        assertEquals("SEC-SSH-01", creada.codigo());
        assertEquals(TipoReglaConfiguracion.DEBE_CONTENER, creada.tipo());
        assertEquals(SeveridadRegla.ALTA, creada.severidad());
        assertEquals(EstadoRegla.ACTIVA, creada.estado());
    }

    @Test
    void rechazaCodigoDuplicadoDentroDeLaMismaBaseline() {
        FakeBaselineRepository baselines = new FakeBaselineRepository();
        FakeReglaRepository reglas = new FakeReglaRepository();
        reglas.items.add(new ReglaBaseline(
                1L, 1L, "SEC-SSH-01", "SSH", "desc", TipoReglaConfiguracion.DEBE_CONTENER,
                "ip ssh version 2", SeveridadRegla.ALTA, "recom", EstadoRegla.ACTIVA
        ));
        ReglaBaselineService service = new ReglaBaselineService(reglas, baselines, (actor, accion, detalle) -> {});

        assertThrows(IllegalStateException.class, () -> service.crear(
                1L, "sec-ssh-01", "Otra", "desc", "DEBE_CONTENER", "ssh", "MEDIA", "recom", "admin"
        ));
    }

    private static class FakeBaselineRepository implements BaselineRepository {
        private final BaselineConfiguracion baseline = new BaselineConfiguracion(
                1L, "Hardening Cisco IOS", "Base", 1, 1L, 2L, EstadoBaseline.ACTIVO
        );
        @Override public BaselineConfiguracion guardar(BaselineConfiguracion b) { return b; }
        @Override public List<BaselineConfiguracion> listar() { return List.of(baseline); }
        @Override public List<BaselineConfiguracion> listarPorOrganizacion(Long id) { return id == 2L ? List.of(baseline) : List.of(); }
        @Override public Optional<BaselineConfiguracion> buscarPorId(Long id) { return id == 1L ? Optional.of(baseline) : Optional.empty(); }
        @Override public Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return id == 1L && org == 2L ? Optional.of(baseline) : Optional.empty(); }
        @Override public boolean existeNombreEnOrganizacion(String nombre, Long org) { return false; }
    }

    private static class FakeReglaRepository implements ReglaBaselineRepository {
        private final List<ReglaBaseline> items = new ArrayList<>();
        @Override public ReglaBaseline guardar(ReglaBaseline r) {
            ReglaBaseline creada = new ReglaBaseline((long) items.size() + 1, r.baselineId(), r.codigo(), r.nombre(), r.descripcion(), r.tipo(), r.patron(), r.severidad(), r.recomendacion(), r.estado());
            items.add(creada); return creada;
        }
        @Override public List<ReglaBaseline> listar() { return List.copyOf(items); }
        @Override public List<ReglaBaseline> listarPorOrganizacion(Long id) { return List.copyOf(items); }
        @Override public List<ReglaBaseline> listarPorBaseline(Long id) { return items.stream().filter(r -> r.baselineId().equals(id)).toList(); }
        @Override public List<ReglaBaseline> listarPorBaselineYOrganizacion(Long id, Long org) { return listarPorBaseline(id); }
        @Override public Optional<ReglaBaseline> buscarPorId(Long id) { return items.stream().filter(r -> r.id().equals(id)).findFirst(); }
        @Override public Optional<ReglaBaseline> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id); }
        @Override public boolean existeCodigoEnBaseline(String codigo, Long baselineId) { return items.stream().anyMatch(r -> r.baselineId().equals(baselineId) && r.codigo().equalsIgnoreCase(codigo)); }
        @Override public long contarPorBaseline(Long baselineId) { return listarPorBaseline(baselineId).size(); }
    }
}
