package ar.edu.uai.tfi.auditcore.application.storage;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort.AlmacenamientoException;
import ar.edu.uai.tfi.auditcore.application.port.ExportadorBaseDatosPort;
import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Bloque 7 · Respaldos: nombre, ubicación, hash, catálogo y registro en la bitácora de transacciones. */
class RespaldoServiceTest {

    private final EnMemoria.Configuraciones configuraciones = new EnMemoria.Configuraciones();
    private final EnMemoria.Reportes reportes = new EnMemoria.Reportes();
    private final EnMemoria.Archivos archivos = new EnMemoria.Archivos(configuraciones, reportes);
    private final EnMemoria.Almacen almacen = new EnMemoria.Almacen();
    private final List<Transaccion> bitacora = new ArrayList<>();
    private final List<ExportadorBaseDatosPort.MetadatosRespaldo> pedidos = new ArrayList<>();

    private final ExportadorBaseDatosPort exportador = (destino, metadatos) -> {
        pedidos.add(metadatos);
        try {
            destino.write("ZIP-DE-PRUEBA".getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        Map<String, String> versiones = new LinkedHashMap<>();
        versiones.put("flyway_historial_audit_core", "7");
        versiones.put("flyway_historial_management", "4");
        return new ExportadorBaseDatosPort.ResumenExportacion(List.of(
                new ExportadorBaseDatosPort.TablaExportada("usuarios", 3, "a"),
                new ExportadorBaseDatosPort.TablaExportada("dispositivos_red", 5, "b")), versiones);
    };

    private RespaldoService servicio() {
        RegistroArchivos registro = new RegistroArchivos(archivos, bitacora::add);
        RepositorioArchivosService repositorio =
                new RepositorioArchivosService(almacen, archivos, configuraciones, reportes, registro);
        RespaldoService service = new RespaldoService(exportador, repositorio, almacen, registro);
        service.reloj = Clock.fixed(Instant.parse("2026-10-11T22:30:15Z"), ZoneOffset.UTC);
        service.versionAplicacion = "sha-abc123";
        return service;
    }

    @Test
    void generaElRespaldoEnElRepositorioConHoraArgentinaYLoRegistra() {
        ArchivoRepositorio respaldo = servicio().generar("admin");

        assertEquals(TipoArchivo.RESPALDO, respaldo.tipo());
        assertNull(respaldo.entidadId());
        assertEquals("auditnet-respaldo-20261011-193015.zip", respaldo.nombre());
        assertEquals("respaldos/2026/10/auditnet-respaldo-20261011-193015.zip", respaldo.ruta());
        assertEquals("2 tablas, 8 filas · esquema audit-core V7, management V4", respaldo.detalle());
        assertEquals(RepositorioArchivosService.sha256("ZIP-DE-PRUEBA".getBytes(StandardCharsets.UTF_8)), respaldo.sha256());
        assertEquals("sha-abc123", pedidos.get(0).versionAplicacion());
        assertEquals("admin", pedidos.get(0).generadoPor());

        Transaccion registro = bitacora.get(0);
        assertEquals("RESPALDO", registro.entidad());
        assertEquals(OperacionTransaccion.EXPORTACION, registro.operacion());
        assertEquals(respaldo.id(), registro.entidadId());
        assertEquals(8L, ((Map<?, ?>) registro.valorNuevo()).get("filas"));
    }

    @Test
    void elRespaldoSeDescargaVerificadoYApareceEnElListado() {
        RespaldoService service = servicio();
        ArchivoRepositorio respaldo = service.generar("admin");

        assertEquals("ZIP-DE-PRUEBA", new String(service.descargar(respaldo.id()).contenido(), StandardCharsets.UTF_8));
        assertEquals(List.of(respaldo.id()), service.listar().stream().map(ArchivoRepositorio::id).toList());
    }

    @Test
    void sinRepositorioConfiguradoNoGenera() {
        almacen.configurado = false;

        assertThrows(AlmacenamientoException.class, () -> servicio().generar("admin"));
        assertTrue(pedidos.isEmpty());
        assertTrue(bitacora.isEmpty());
    }
}
