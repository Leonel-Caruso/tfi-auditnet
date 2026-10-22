package ar.edu.uai.tfi.auditcore.application.config;

import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import ar.edu.uai.tfi.auditcore.application.storage.RegistroArchivos;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionServiceTest {

    @Test
    void importacionRegistraResumenConHashSinCopiarElContenido() {
        List<Transaccion> transacciones = new ArrayList<>();
        ConfiguracionService service = new ConfiguracionService(
                new FakeConfiguracionRepository(),
                new FakeDispositivoRepository(),
                new NormalizacionConfiguracionService(),
                transacciones::add,
                sinRepositorio(),
                EnMemoria.planSinLimites()
        );
        String contenido = "hostname RTR-CORE-01\nenable secret 5 $1$abc$xyz\n";

        ConfiguracionDispositivo creada = service.importarParaOrganizacion(1L, 2L, contenido, "TXT", "r.txt", "analista");

        assertEquals(1, transacciones.size());
        Transaccion importacion = transacciones.get(0);
        assertEquals("CONFIGURACION", importacion.entidad());
        assertEquals(OperacionTransaccion.IMPORTACION, importacion.operacion());
        assertEquals(creada.id(), importacion.entidadId());

        Map<?, ?> resumen = (Map<?, ?>) importacion.valorNuevo();
        assertEquals(ConfiguracionService.sha256(contenido), resumen.get("sha256"));
        assertEquals(64, ((String) resumen.get("sha256")).length());
        assertFalse(resumen.toString().contains("enable secret"), "La bitácora no debe copiar la configuración");
    }

    @Test
    void importaNuevaVersionConOriginalYNormalizada() {
        FakeConfiguracionRepository configuraciones = new FakeConfiguracionRepository();
        FakeDispositivoRepository dispositivos = new FakeDispositivoRepository();
        ConfiguracionService service = new ConfiguracionService(
                configuraciones,
                dispositivos,
                new NormalizacionConfiguracionService(),
                transaccion -> {},
                sinRepositorio(),
                EnMemoria.planSinLimites()
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
                transaccion -> {},
                sinRepositorio(),
                EnMemoria.planSinLimites()
        );

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> service.importarParaOrganizacion(
                        1L, 99L, "hostname x", "TEXTO", null, "analista"
                )
        );
    }

    @Test
    void importacionGuardaElArchivoOriginalConElMismoHashQueLaBitacora() {
        List<Transaccion> transacciones = new ArrayList<>();
        EnMemoria.Almacen almacen = new EnMemoria.Almacen();
        EnMemoria.Archivos archivos = new EnMemoria.Archivos(new EnMemoria.Configuraciones(), new EnMemoria.Reportes());
        ConfiguracionService service = new ConfiguracionService(new FakeConfiguracionRepository(),
                new FakeDispositivoRepository(), new NormalizacionConfiguracionService(), transacciones::add,
                new RepositorioArchivosService(almacen, archivos, archivos.configuraciones, archivos.reportes, new RegistroArchivos(archivos, t -> {})),
                EnMemoria.planSinLimites());
        String contenido = "hostname RTR-CORE-01\r\n! ñandú\r\n";

        ConfiguracionDispositivo creada = service.importarParaOrganizacion(1L, 2L, contenido, "CFG", "rtr.cfg", "analista");

        ArchivoRepositorio archivo = archivos.items.get(0);
        assertEquals(TipoArchivo.CONFIGURACION, archivo.tipo());
        assertEquals(creada.id(), archivo.entidadId());
        assertEquals("CFG-001-v1.cfg", archivo.nombre());
        assertEquals("configuraciones/org-2/dispositivo-1/CFG-001-v1-" + archivo.sha256().substring(0, 12) + ".cfg", archivo.ruta());
        assertArrayEquals(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8), almacen.archivos.get(archivo.ruta()));
        assertEquals(((Map<?, ?>) transacciones.get(0).valorNuevo()).get("sha256"), archivo.sha256(),
                "El hash del archivo coincide con el registrado en la bitácora");
        assertEquals("analista", archivo.usuario());
    }

    @Test
    void siElRepositorioNoRespondeLaImportacionFalla() {
        EnMemoria.Almacen almacen = new EnMemoria.Almacen();
        almacen.caido = true;
        EnMemoria.Archivos archivos = new EnMemoria.Archivos(new EnMemoria.Configuraciones(), new EnMemoria.Reportes());
        ConfiguracionService service = new ConfiguracionService(new FakeConfiguracionRepository(),
                new FakeDispositivoRepository(), new NormalizacionConfiguracionService(), t -> {},
                new RepositorioArchivosService(almacen, archivos, archivos.configuraciones, archivos.reportes, new RegistroArchivos(archivos, t -> {})),
                EnMemoria.planSinLimites());

        assertThrows(AlmacenArchivosPort.AlmacenamientoException.class,
                () -> service.importarParaOrganizacion(1L, 2L, "hostname x", "TEXTO", null, "analista"));
        assertTrue(archivos.items.isEmpty());
    }

    private static RepositorioArchivosService sinRepositorio() {
        EnMemoria.Almacen almacen = new EnMemoria.Almacen();
        almacen.configurado = false;
        EnMemoria.Archivos archivos = new EnMemoria.Archivos(new EnMemoria.Configuraciones(), new EnMemoria.Reportes());
        return new RepositorioArchivosService(almacen, archivos, archivos.configuraciones, archivos.reportes, new RegistroArchivos(archivos, t -> {}));
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
        @Override public DispositivoRed actualizar(DispositivoRed dispositivo) { return dispositivo; }
        @Override public boolean existeIdentificadorEnOrganizacion(String identificador, Long org, Long excluirId) { return false; }
    }
}
