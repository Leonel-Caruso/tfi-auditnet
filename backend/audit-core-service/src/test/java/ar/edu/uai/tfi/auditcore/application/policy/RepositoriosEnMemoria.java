package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorios en memoria para probar los servicios de baselines y reglas sin base de datos. */
final class RepositoriosEnMemoria {

    private RepositoriosEnMemoria() {
    }

    static final class Baselines implements BaselineRepository {
        final List<BaselineConfiguracion> items = new ArrayList<>();
        private long secuencia = 100;

        @Override
        public BaselineConfiguracion guardar(BaselineConfiguracion b) {
            Long id = b.id() == null ? ++secuencia : b.id();
            BaselineConfiguracion creada = new BaselineConfiguracion(id, b.nombre(), b.descripcion(), b.version(),
                    b.tipoDispositivoId(), b.organizacionId(), b.estado());
            items.add(creada);
            return creada;
        }

        @Override
        public BaselineConfiguracion actualizar(BaselineConfiguracion b) {
            items.replaceAll(existente -> existente.id().equals(b.id()) ? b : existente);
            return b;
        }

        @Override public List<BaselineConfiguracion> listar() { return List.copyOf(items); }
        @Override public List<BaselineConfiguracion> listarPorOrganizacion(Long id) { return items.stream().filter(b -> b.organizacionId().equals(id)).toList(); }
        @Override public Optional<BaselineConfiguracion> buscarPorId(Long id) { return items.stream().filter(b -> b.id().equals(id)).findFirst(); }
        @Override public Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return items.stream().filter(b -> b.id().equals(id) && b.organizacionId().equals(org)).findFirst(); }
        @Override public boolean existeNombreEnOrganizacion(String nombre, Long org) { return items.stream().anyMatch(b -> b.organizacionId().equals(org) && b.nombre().equalsIgnoreCase(nombre)); }
    }

    static final class Reglas implements ReglaBaselineRepository {
        final List<ReglaBaseline> items = new ArrayList<>();
        private long secuencia = 500;

        @Override
        public ReglaBaseline guardar(ReglaBaseline r) {
            Long id = r.id() == null ? ++secuencia : r.id();
            ReglaBaseline creada = new ReglaBaseline(id, r.baselineId(), r.codigo(), r.nombre(), r.descripcion(),
                    r.tipo(), r.patron(), r.valorEsperado(), r.severidad(), r.impacto(), r.recomendacion(), r.estado());
            items.add(creada);
            return creada;
        }

        @Override
        public ReglaBaseline actualizar(ReglaBaseline r) {
            items.replaceAll(existente -> existente.id().equals(r.id()) ? r : existente);
            return r;
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

    /** Solo cuenta auditorías por baseline: es lo que usan los servicios de políticas. */
    static final class Auditorias implements AuditoriaRepository {
        final Map<Long, Long> porBaseline = new HashMap<>();

        @Override public AuditoriaConfiguracion guardar(AuditoriaConfiguracion a) { throw new UnsupportedOperationException(); }
        @Override public List<AuditoriaConfiguracion> listarHistorial() { return List.of(); }
        @Override public List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long id) { return List.of(); }
        @Override public Optional<AuditoriaConfiguracion> buscarPorId(Long id) { return Optional.empty(); }
        @Override public Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return Optional.empty(); }
        @Override public long contarPorBaseline(Long baselineId) { return porBaseline.getOrDefault(baselineId, 0L); }
        @Override public Map<Long, Long> contarPorBaselines() { return Map.copyOf(porBaseline); }
    }
}
