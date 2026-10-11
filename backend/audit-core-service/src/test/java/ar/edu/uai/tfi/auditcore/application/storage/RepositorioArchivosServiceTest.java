package ar.edu.uai.tfi.auditcore.application.storage;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort.AlmacenamientoException;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.support.EnMemoria;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/** Bloque 7 · Repositorio de archivos: rutas, hash SHA-256, verificación al descargar y archivos pendientes. */
class RepositorioArchivosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-11T22:00:00Z");

    private final EnMemoria.Configuraciones configuraciones = new EnMemoria.Configuraciones();
    private final EnMemoria.Reportes reportes = new EnMemoria.Reportes();
    private final EnMemoria.Archivos archivos = new EnMemoria.Archivos(configuraciones, reportes);
    private final EnMemoria.Almacen almacen = new EnMemoria.Almacen();
    private final List<Transaccion> bitacora = new ArrayList<>();
    private final RepositorioArchivosService service =
            new RepositorioArchivosService(almacen, archivos, configuraciones, reportes, new RegistroArchivos(archivos, bitacora::add));

    @Test
    void guardaLaConfiguracionConSuHashYMetadatos() {
        ConfiguracionDispositivo configuracion = configuracion(7L, "hostname RTR\n");

        ArchivoRepositorio archivo = service.guardarConfiguracion(configuracion, "analista").orElseThrow();

        assertEquals("configuraciones/org-2/dispositivo-4/CFG-007-v3-" + archivo.sha256().substring(0, 12) + ".txt", archivo.ruta());
        assertEquals("CFG-007-v3.txt", archivo.nombre());
        assertEquals(RepositorioArchivosService.sha256("hostname RTR\n".getBytes(StandardCharsets.UTF_8)), archivo.sha256());
        assertEquals(13, archivo.tamanioBytes());
        assertEquals(archivo.sha256(), almacen.metadatos.get(archivo.ruta()).get("sha256"));
        assertEquals("CONFIGURACION", almacen.metadatos.get(archivo.ruta()).get("tipo"));
    }

    @Test
    void sinRepositorioConfiguradoNoGuardaYQuedaPendiente() {
        almacen.configurado = false;
        configuraciones.items.add(configuracion(7L, "hostname RTR"));

        assertTrue(service.guardarConfiguracion(configuraciones.items.get(0), "analista").isEmpty());
        RepositorioArchivosService.EstadoRepositorio estado = service.estado();
        assertFalse(estado.configurado());
        assertEquals(1, estado.configuracionesPendientes());
        assertThrows(AlmacenamientoException.class, () -> service.sincronizarPendientes("admin"));
    }

    @Test
    void alDescargarVerificaElHashYDetectaAlteraciones() {
        ArchivoRepositorio archivo = service.guardarConfiguracion(configuracion(7L, "hostname RTR"), "analista").orElseThrow();

        RepositorioArchivosService.ArchivoVerificado verificado = service.descargar(TipoArchivo.CONFIGURACION, 7L);
        assertEquals("hostname RTR", new String(verificado.contenido(), StandardCharsets.UTF_8));

        almacen.archivos.put(archivo.ruta(), "hostname RTR\nno shutdown".getBytes(StandardCharsets.UTF_8));
        assertThrows(RepositorioArchivosService.ArchivoAlteradoException.class,
                () -> service.descargar(TipoArchivo.CONFIGURACION, 7L));
        assertThrows(NoSuchElementException.class, () -> service.descargar(TipoArchivo.CONFIGURACION, 99L));
    }

    @Test
    void sincronizaLasConfiguracionesYReportesPendientes() {
        configuraciones.items.add(configuracion(1L, "hostname A"));
        configuraciones.items.add(configuracion(2L, "hostname B"));
        reportes.guardar(reporte(), List.of(1L));
        service.guardarConfiguracion(configuraciones.items.get(0), "analista");

        RepositorioArchivosService.ResultadoSincronizacion resultado = service.sincronizarPendientes("admin");

        assertEquals(1, resultado.configuraciones());
        assertEquals(1, resultado.reportes());
        assertEquals(0, resultado.pendientes());
        assertTrue(almacen.archivos.keySet().stream().anyMatch(ruta -> ruta.startsWith("reportes/org-2/REP-001-")));
        Transaccion registro = bitacora.get(0);
        assertEquals("ARCHIVO", registro.entidad());
        assertEquals(OperacionTransaccion.ALTA, registro.operacion());
        assertEquals("admin", registro.actor());
        assertEquals(0, service.sincronizarPendientes("admin").configuraciones(), "una segunda pasada no repite");
        assertEquals(1, bitacora.size(), "sin archivos nuevos no se registra nada");
    }

    @Test
    void siElArchivoYaEstabaSubidoConElMismoContenidoSoloLoRegistra() {
        String hash = RepositorioArchivosService.sha256("hostname RTR".getBytes(StandardCharsets.UTF_8)).substring(0, 12);
        almacen.archivos.put("configuraciones/org-2/dispositivo-4/CFG-007-v3-" + hash + ".txt",
                "hostname RTR".getBytes(StandardCharsets.UTF_8));

        assertTrue(service.guardarConfiguracion(configuracion(7L, "hostname RTR"), "analista").isPresent());
        assertEquals(1, almacen.archivos.size());
    }

    @Test
    void unIdRepetidoConOtroContenidoNoPisaElArchivoAnterior() {
        // Después de restaurar un respaldo los ids vuelven atrás: la ruta lleva el hash y no choca.
        service.guardarConfiguracion(configuracion(7L, "hostname VIEJO"), "analista");
        archivos.items.clear();

        assertTrue(service.guardarConfiguracion(configuracion(7L, "hostname NUEVO"), "analista").isPresent());
        assertEquals(2, almacen.archivos.size());
    }

    @Test
    void unArchivoDistintoConLaMismaRutaNoSeReemplazaNiSeRegistra() {
        String hash = RepositorioArchivosService.sha256("hostname OTRO".getBytes(StandardCharsets.UTF_8)).substring(0, 12);
        almacen.archivos.put("configuraciones/org-2/dispositivo-4/CFG-008-v3-" + hash + ".txt", "distinto".getBytes(StandardCharsets.UTF_8));

        AlmacenamientoException error = assertThrows(AlmacenamientoException.class,
                () -> service.guardarConfiguracion(configuracion(8L, "hostname OTRO"), "analista"));

        assertEquals(409, error.estadoHttp());
        assertTrue(archivos.items.isEmpty());
    }

    @Test
    void elEstadoInformaCantidadesYBytesPorTipo() {
        service.guardarConfiguracion(configuracion(7L, "1234567890"), "analista");

        Map<TipoArchivo, long[]> totales = service.estado().totales();

        assertArrayEquals(new long[]{1, 10}, totales.get(TipoArchivo.CONFIGURACION));
        assertArrayEquals(new long[]{0, 0}, totales.get(TipoArchivo.RESPALDO));
    }

    private static ConfiguracionDispositivo configuracion(Long id, String contenido) {
        return new ConfiguracionDispositivo(id, 4L, 2L, 3, FormatoConfiguracion.TXT, "rtr.txt", contenido, contenido,
                AHORA, "analista");
    }

    private static ReporteAuditoria reporte() {
        ContenidoReporte.Resumen resumen = new ContenidoReporte.Resumen(1, 1, 2, 2, 100, 0, 0,
                Map.of(), Map.of(), "NINGUNA", "SIN_DESVIOS");
        return new ReporteAuditoria(null, 2L, "Reporte de prueba",
                new CriteriosReporte(2L, AHORA, AHORA, List.of(), List.of(), List.of(), List.of()),
                new ContenidoReporte(resumen, List.of(), List.of(), List.of()), 1, 0, "auditor", AHORA);
    }
}
