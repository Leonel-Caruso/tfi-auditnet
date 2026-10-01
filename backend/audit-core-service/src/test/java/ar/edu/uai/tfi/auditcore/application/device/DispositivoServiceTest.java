package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispositivoServiceTest {

    @Test
    void crearDispositivoRegistraTransaccionDeAlta() {
        List<Transaccion> transacciones = new ArrayList<>();
        DispositivoService service = new DispositivoService(new FakeRepository(), transacciones::add);

        DispositivoRed creado = service.crear("Router Central", "rtr-core-01", 1L, "Cisco", 2L, 1L, "ALTA", "admin");

        assertEquals(1, transacciones.size());
        Transaccion alta = transacciones.get(0);
        assertEquals("DISPOSITIVO", alta.entidad());
        assertEquals(creado.id(), alta.entidadId());
        assertEquals(OperacionTransaccion.ALTA, alta.operacion());
        assertEquals(2L, alta.organizacionId());
        assertNull(alta.valorAnterior());
        assertEquals(creado, alta.valorNuevo());
        assertEquals("admin", alta.actor());
    }

    @Test
    void creaDispositivoNormalizandoIdentificador() {
        DispositivoService service = new DispositivoService(new FakeRepository(), transaccion -> {});

        DispositivoRed creado = service.crear("Router Central", " rtr-core-01 ", 1L, "Cisco", 2L, 1L, "alta", "admin");

        assertEquals("RTR-CORE-01", creado.identificador());
        assertEquals(CriticidadDispositivo.ALTA, creado.criticidad());
        assertEquals(EstadoDispositivo.ACTIVO, creado.estado());
    }

    @Test
    void rechazaIdentificadorDuplicadoEnLaMismaOrganizacion() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        DispositivoService service = new DispositivoService(repository, transaccion -> {});

        assertThrows(IllegalStateException.class, () ->
                service.crear("Otro router", "rtr-core-01", 1L, "Cisco", 2L, 1L, "MEDIA", "admin"));
    }

    @Test
    void permiteElMismoIdentificadorEnOtraOrganizacion() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        DispositivoService service = new DispositivoService(repository, transaccion -> {});

        DispositivoRed creado = service.crear("Router de otro cliente", "rtr-core-01", 1L, "Cisco", 3L, null, "MEDIA", "admin");

        assertEquals("RTR-CORE-01", creado.identificador());
        assertEquals(3L, creado.organizacionId());
    }

    @Test
    void modificarRegistraValorAnteriorYNuevo() {
        FakeRepository repository = new FakeRepository();
        DispositivoRed original = dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO);
        repository.dispositivos.add(original);
        List<Transaccion> transacciones = new ArrayList<>();
        DispositivoService service = new DispositivoService(repository, transacciones::add);

        DispositivoRed modificado = service.modificar(1L, "Router renombrado", "rtr-core-01", 1L, "Cisco", 1L, "CRITICA", "admin");

        assertEquals("Router renombrado", modificado.nombre());
        assertEquals(CriticidadDispositivo.CRITICA, modificado.criticidad());
        assertEquals(2L, modificado.organizacionId());
        assertEquals(1, transacciones.size());
        assertEquals(OperacionTransaccion.MODIFICACION, transacciones.get(0).operacion());
        assertEquals(original, transacciones.get(0).valorAnterior());
        assertEquals(modificado, transacciones.get(0).valorNuevo());
    }

    @Test
    void modificarSinCambiosNoRegistraTransaccion() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        List<Transaccion> transacciones = new ArrayList<>();
        DispositivoService service = new DispositivoService(repository, transacciones::add);

        service.modificar(1L, "Router", "RTR-CORE-01", 1L, "Cisco", 1L, "MEDIA", "admin");

        assertTrue(transacciones.isEmpty());
    }

    @Test
    void modificarRechazaIdentificadorDeOtroDispositivoDeLaOrganizacion() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        repository.dispositivos.add(dispositivo(2L, "SW-ACC-01", 2L, EstadoDispositivo.ACTIVO));
        DispositivoService service = new DispositivoService(repository, transaccion -> {});

        assertThrows(IllegalStateException.class, () ->
                service.modificar(2L, "Switch", "rtr-core-01", 1L, "Cisco", null, "MEDIA", "admin"));
    }

    @Test
    void bajaLogicaRegistraBajaYReactivacionRegistraCambioDeEstado() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        List<Transaccion> transacciones = new ArrayList<>();
        DispositivoService service = new DispositivoService(repository, transacciones::add);

        DispositivoRed inactivo = service.cambiarEstado(1L, "inactivo", "admin");
        DispositivoRed activo = service.cambiarEstado(1L, "ACTIVO", "admin");

        assertEquals(EstadoDispositivo.INACTIVO, inactivo.estado());
        assertEquals(EstadoDispositivo.ACTIVO, activo.estado());
        assertEquals(OperacionTransaccion.BAJA_LOGICA, transacciones.get(0).operacion());
        assertEquals(OperacionTransaccion.CAMBIO_ESTADO, transacciones.get(1).operacion());
        assertEquals("ACTIVO -> INACTIVO", transacciones.get(0).detalle());
    }

    @Test
    void rechazaCambiarAlMismoEstadoOAUnEstadoInvalido() {
        FakeRepository repository = new FakeRepository();
        repository.dispositivos.add(dispositivo(1L, "RTR-CORE-01", 2L, EstadoDispositivo.ACTIVO));
        DispositivoService service = new DispositivoService(repository, transaccion -> {});

        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(1L, "ACTIVO", "admin"));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1L, "BORRADO", "admin"));
    }

    private static DispositivoRed dispositivo(Long id, String identificador, Long organizacionId, EstadoDispositivo estado) {
        return new DispositivoRed(id, "Router", identificador, 1L, "Cisco", organizacionId, 1L,
                CriticidadDispositivo.MEDIA, estado);
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
        public DispositivoRed actualizar(DispositivoRed dispositivo) {
            dispositivos.replaceAll(existente -> existente.id().equals(dispositivo.id()) ? dispositivo : existente);
            return dispositivo;
        }

        @Override
        public List<DispositivoRed> listar() {
            return List.copyOf(dispositivos);
        }

        @Override
        public List<DispositivoRed> listarPorOrganizacion(Long organizacionId) {
            return dispositivos.stream().filter(d -> d.organizacionId().equals(organizacionId)).toList();
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
        public boolean existeIdentificadorEnOrganizacion(String identificador, Long organizacionId, Long excluirId) {
            return dispositivos.stream()
                    .filter(d -> d.organizacionId().equals(organizacionId))
                    .filter(d -> !d.id().equals(excluirId))
                    .anyMatch(d -> d.identificador().equalsIgnoreCase(identificador));
        }
    }
}
