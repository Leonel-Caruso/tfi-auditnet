package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DispositivoServiceTest {

    @Test
    void creaDispositivoNormalizandoIdentificador() {
        FakeRepository repository = new FakeRepository();
        DispositivoService service = new DispositivoService(repository, (actor, accion, detalle) -> {});

        DispositivoRed creado = service.crear(
                "Router Central",
                " rtr-core-01 ",
                1L,
                "Cisco",
                2L,
                1L,
                "alta",
                "admin"
        );

        assertEquals("RTR-CORE-01", creado.identificador());
        assertEquals(CriticidadDispositivo.ALTA, creado.criticidad());
        assertEquals(EstadoDispositivo.ACTIVO, creado.estado());
    }

    @Test
    void rechazaIdentificadorDuplicado() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(new DispositivoRed(
                1L,
                "Router existente",
                "RTR-CORE-01",
                1L,
                "Cisco",
                2L,
                1L,
                CriticidadDispositivo.MEDIA,
                EstadoDispositivo.ACTIVO
        ));

        DispositivoService service = new DispositivoService(repository, (actor, accion, detalle) -> {});

        assertThrows(IllegalStateException.class, () -> service.crear(
                "Otro router",
                "rtr-core-01",
                1L,
                "Cisco",
                2L,
                1L,
                "MEDIA",
                "admin"
        ));
    }

    private static class FakeRepository implements DispositivoRepository {
        private final List<DispositivoRed> dispositivos = new ArrayList<>();

        @Override
        public DispositivoRed guardar(DispositivoRed dispositivo) {
            DispositivoRed creado = new DispositivoRed(
                    (long) dispositivos.size() + 1,
                    dispositivo.nombre(),
                    dispositivo.identificador(),
                    dispositivo.tipoDispositivoId(),
                    dispositivo.fabricante(),
                    dispositivo.organizacionId(),
                    dispositivo.sedeId(),
                    dispositivo.criticidad(),
                    dispositivo.estado()
            );
            dispositivos.add(creado);
            return creado;
        }

        @Override
        public List<DispositivoRed> listar() {
            return List.copyOf(dispositivos);
        }

        @Override
        public List<DispositivoRed> listarPorOrganizacion(Long organizacionId) {
            return dispositivos.stream()
                    .filter(d -> d.organizacionId().equals(organizacionId))
                    .toList();
        }

        @Override
        public Optional<DispositivoRed> buscarPorId(Long id) {
            return dispositivos.stream().filter(d -> d.id().equals(id)).findFirst();
        }

        @Override
        public Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
            return dispositivos.stream()
                    .filter(d -> d.id().equals(id) && d.organizacionId().equals(organizacionId))
                    .findFirst();
        }

        @Override
        public boolean existePorIdentificador(String identificador) {
            return dispositivos.stream()
                    .anyMatch(d -> d.identificador().equalsIgnoreCase(identificador));
        }
    }
}
