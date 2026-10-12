package ar.edu.uai.tfi.auditcore.application.storage;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort.AlmacenamientoException;
import ar.edu.uai.tfi.auditcore.application.port.ExportadorBaseDatosPort;
import ar.edu.uai.tfi.auditcore.application.port.ExportadorBaseDatosPort.MetadatosRespaldo;
import ar.edu.uai.tfi.auditcore.application.port.ExportadorBaseDatosPort.ResumenExportacion;
import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Respaldos lógicos de la base generados desde el panel: datos, versión del esquema y guion de restauración en un
 * ZIP que se guarda en el repositorio de archivos con su SHA-256. Complementan los respaldos automáticos de Azure.
 */
@ApplicationScoped
public class RespaldoService {

    static final long TAMANIO_MAXIMO = 100L * 1024 * 1024;
    static final int LISTADO = 50;
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter CARPETA = DateTimeFormatter.ofPattern("yyyy/MM");

    private final ExportadorBaseDatosPort exportador;
    private final RepositorioArchivosService repositorio;
    private final AlmacenArchivosPort almacen;
    private final RegistroArchivos registro;
    Clock reloj = Clock.systemUTC();

    @ConfigProperty(name = "app.version", defaultValue = "local")
    String versionAplicacion = "local";

    public RespaldoService(ExportadorBaseDatosPort exportador, RepositorioArchivosService repositorio,
                           AlmacenArchivosPort almacen, RegistroArchivos registro) {
        this.exportador = exportador;
        this.repositorio = repositorio;
        this.almacen = almacen;
        this.registro = registro;
    }

    /**
     * Sin transacción a propósito: la exportación usa su propia conexión de solo lectura y la subida puede tardar.
     * Al final, el catálogo y la bitácora se guardan juntos en una transacción corta.
     */
    public ArchivoRepositorio generar(String usuario) {
        if (!almacen.configurado()) {
            throw new AlmacenamientoException("El repositorio de archivos no está configurado: no hay dónde guardar el respaldo.",
                    503, null);
        }
        Instant ahora = reloj.instant();
        String nombre = "auditnet-respaldo-" + NOMBRE.format(ahora.atZone(ZONA)) + ".zip";
        String ruta = "respaldos/" + CARPETA.format(ahora.atZone(ZONA)) + "/" + nombre;

        Path temporal = null;
        try {
            temporal = Files.createTempFile("auditnet-respaldo", ".zip");
            ResumenExportacion resumen;
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                resumen = exportador.exportar(salida, new MetadatosRespaldo(nombre, usuario, ahora, versionAplicacion));
            }
            if (Files.size(temporal) > TAMANIO_MAXIMO) {
                throw new IllegalStateException("El respaldo supera los 100 MB: usar los respaldos automáticos de Azure.");
            }
            byte[] contenido = Files.readAllBytes(temporal);
            String detalle = detalle(resumen);
            ArchivoRepositorio subido = repositorio.subir(TipoArchivo.RESPALDO, null, null, ruta, nombre,
                    "application/zip", contenido, usuario, detalle);

            Map<String, Object> valor = new LinkedHashMap<>();
            valor.put("archivo", nombre);
            valor.put("sha256", subido.sha256());
            valor.put("bytes", subido.tamanioBytes());
            valor.put("tablas", resumen.tablas().size());
            valor.put("filas", resumen.filas());
            valor.put("versionesEsquema", resumen.versionesEsquema());
            return registro.guardar(subido, guardado -> new Transaccion("RESPALDO", guardado.id(),
                    OperacionTransaccion.EXPORTACION, null, null, valor, usuario, detalle));
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo preparar el archivo del respaldo.", exception);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignorada) {
                    // El temporal queda en el directorio del sistema; no afecta al respaldo.
                }
            }
        }
    }

    public List<ArchivoRepositorio> listar() {
        return repositorio.listar(TipoArchivo.RESPALDO, LISTADO);
    }

    public RepositorioArchivosService.ArchivoVerificado descargar(Long id) {
        return repositorio.descargarPorId(id, TipoArchivo.RESPALDO);
    }

    /** "14 tablas, 1.234 filas · esquema management V4, audit-core V7" */
    static String detalle(ResumenExportacion resumen) {
        String versiones = resumen.versionesEsquema().entrySet().stream()
                .map(entrada -> servicio(entrada.getKey()) + " V" + entrada.getValue())
                .collect(Collectors.joining(", "));
        return resumen.tablas().size() + " tablas, " + resumen.filas() + " filas · esquema " + versiones;
    }

    private static String servicio(String historial) {
        return historial.replace("flyway_historial_", "").replace('_', '-');
    }
}
