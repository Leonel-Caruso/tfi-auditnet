package ar.edu.uai.tfi.auditcore.application.config;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionServiceTest {

    @Test
    void importaNuevaVersionConOriginalYNormalizada() {
        FakeConfiguracionRepository configuraciones = new FakeConfiguracionRepository();
        FakeDispositivoRepository dispositivos = new FakeDispositivoRepository();
        ConfiguracionService service = new ConfiguracionService(
                configuraciones,
                dispositivos,
                new NormalizacionConfiguracionService(),
                (actor, accion, detalle) -> {}
        );

        ConfiguracionDispositivo primera = service.importarParaOrganizacion(
                1L, 2L,
                "hostname RTR-CORE-01\r\nip ssh version 2   \r\n",
                "txt",
                "router.txt",
                "analista"
        );

        ConfiguracionDispositivo segunda = service.importarParaOrganizacion(
                1L, 2L,
                "hostname RTR-CORE-01\nip ssh version 2\n",
                "CFG",
                "router.cfg",
                "analista"
        );

        assertEquals(1, primera.version());
        assertEquals(2, segunda.version());
        assertEquals(FormatoConfiguracion.TXT, primera.formato());
        assertEquals("hostname RTR-CORE-01\r\nip ssh version 2   \r\n", primera.contenidoOriginal());
        assertEquals("hostname RTR-CORE-01\nip ssh version 2", primera.contenidoNormalizado());
    }

    @Test
    void impideImportarDispositivoDeOtraOrganizacion() {
        ConfiguracionService service = new ConfiguracionService(
                new FakeConfiguracionRepository(),
                new FakeDispositivoRepository(),
                new NormalizacionConfiguracionService(),
                (actor, accion, detalle) -> {}
        );

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> service.importarParaOrganizacion(
                        1L, 99L, "hostname x", "TEXTO", null, "analista"
                )
        );
    }

    private static class FakeConfiguracionRepository implements ConfiguracionRepository {
        private final List<ConfiguracionDispositivo> items = new ArrayList<>();

        @Override
        public ConfiguracionDispositivo guardar(ConfiguracionDispositivo c) {
            ConfiguracionDispositivo creada = new ConfiguracionDispositivo(
                    (long) items.size() + 1,
                    c.dispositivoId(),
                    c.organizacionId(),
                    c.version(),
                    c.formato(),
                    c.nombreFuente(),
                    c.contenidoOriginal(),
                    c.contenidoNormalizado(),
                    c.fechaImportacion(),
                    c.usuarioResponsable()
            );
            items.add(creada);
            return creada;
        }

        @Override public List<ConfiguracionDispositivo> listarAuditables() { return List.copyOf(items); }
        @Override public List<ConfiguracionDispositivo> listarAuditablesPorOrganizacion(Long id) {
            return items.stream().filter(c -> c.organizacionId().equals(id)).toList();
        }
        @Override public Optional<ConfiguracionDispositivo> buscarPorId(Long id) {
            return items.stream().filter(c -> c.id().equals(id)).findFirst();
        }
        @Override public Optional<ConfiguracionDispositivo> buscarPorIdYOrganizacion(Long id, Long org) {
            return items.stream().filter(c -> c.id().equals(id) && c.organizacionId().equals(org)).findFirst();
        }
        @Override public int siguienteVersionParaDispositivo(Long dispositivoId) {
            return (int) items.stream().filter(c -> c.dispositivoId().equals(dispositivoId)).count() + 1;
        }
    }

    private static class FakeDispositivoRepository implements DispositivoRepository {
        private final DispositivoRed device = new DispositivoRed(
                1L, "Router Core Central", "RTR-CORE-01", 1L, "Cisco",
                2L, 1L, CriticidadDispositivo.ALTA, EstadoDispositivo.ACTIVO
        );

        @Override public DispositivoRed guardar(DispositivoRed dispositivo) { return dispositivo; }
        @Override public List<DispositivoRed> listar() { return List.of(device); }
        @Override public List<DispositivoRed> listarPorOrganizacion(Long id) {
            return id.equals(2L) ? List.of(device) : List.of();
        }
        @Override public Optional<DispositivoRed> buscarPorId(Long id) {
            return id.equals(1L) ? Optional.of(device) : Optional.empty();
        }
        @Override public Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long org) {
            return id.equals(1L) && org.equals(2L) ? Optional.of(device) : Optional.empty();
        }
        @Override public boolean existePorIdentificador(String identificador) { return false; }
    }
}
