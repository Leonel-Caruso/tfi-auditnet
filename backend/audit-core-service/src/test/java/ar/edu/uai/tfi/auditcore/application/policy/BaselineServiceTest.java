package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaselineServiceTest {

    @Test
    void creaBaselineActivaEnVersionUno() {
        FakeRepository repository = new FakeRepository();
        BaselineService service = new BaselineService(repository, (actor, accion, detalle) -> {});

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
    }

    @Test
    void rechazaNombreDuplicadoDentroDeLaMismaOrganizacion() {
        FakeRepository repository = new FakeRepository();
        repository.items.add(new BaselineConfiguracion(
                1L, "Hardening Cisco IOS", "Base", 1, 1L, 2L, EstadoBaseline.ACTIVO
        ));
        BaselineService service = new BaselineService(repository, (actor, accion, detalle) -> {});

        assertThrows(IllegalStateException.class, () -> service.crear(
                "hardening cisco ios", "Otra", 1L, 2L, "admin"
        ));
    }

    private static class FakeRepository implements BaselineRepository {
        private final List<BaselineConfiguracion> items = new ArrayList<>();

        @Override
        public BaselineConfiguracion guardar(BaselineConfiguracion baseline) {
            BaselineConfiguracion creada = new BaselineConfiguracion(
                    (long) items.size() + 1, baseline.nombre(), baseline.descripcion(), baseline.version(),
                    baseline.tipoDispositivoId(), baseline.organizacionId(), baseline.estado()
            );
            items.add(creada);
            return creada;
        }

        @Override public List<BaselineConfiguracion> listar() { return List.copyOf(items); }
        @Override public List<BaselineConfiguracion> listarPorOrganizacion(Long id) { return items.stream().filter(b -> b.organizacionId().equals(id)).toList(); }
        @Override public Optional<BaselineConfiguracion> buscarPorId(Long id) { return items.stream().filter(b -> b.id().equals(id)).findFirst(); }
        @Override public Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return items.stream().filter(b -> b.id().equals(id) && b.organizacionId().equals(org)).findFirst(); }
        @Override public boolean existeNombreEnOrganizacion(String nombre, Long org) { return items.stream().anyMatch(b -> b.organizacionId().equals(org) && b.nombre().equalsIgnoreCase(nombre)); }
    }
}
