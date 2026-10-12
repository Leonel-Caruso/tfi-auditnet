package ar.edu.uai.tfi.auditcore.application.storage;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort.AlmacenamientoException;
import ar.edu.uai.tfi.auditcore.application.report.ExportadorCsv;
import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.ArchivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReporteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Guarda como evidencia el original de cada configuración importada y el CSV de cada reporte, con su SHA-256 en
 * {@code archivos_repositorio}. Al descargar se recalcula el hash y, si no coincide, el archivo no se entrega.
 */
@ApplicationScoped
public class RepositorioArchivosService {

    static final String CSV = "text/csv; charset=utf-8";
    static final String TEXTO = "text/plain; charset=utf-8";
    static final int LOTE_SINCRONIZACION = 50;

    private final AlmacenArchivosPort almacen;
    private final ArchivoRepository archivoRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final ReporteRepository reporteRepository;
    private final RegistroArchivos registro;

    public RepositorioArchivosService(AlmacenArchivosPort almacen,
                                      ArchivoRepository archivoRepository,
                                      ConfiguracionRepository configuracionRepository,
                                      ReporteRepository reporteRepository,
                                      RegistroArchivos registro) {
        this.almacen = almacen;
        this.archivoRepository = archivoRepository;
        this.configuracionRepository = configuracionRepository;
        this.reporteRepository = reporteRepository;
        this.registro = registro;
    }

    /** Archivo leído del repositorio, ya verificado contra su hash. */
    public record ArchivoVerificado(ArchivoRepositorio archivo, byte[] contenido) {
    }

    /** Estado para el panel de administración. */
    public record EstadoRepositorio(boolean configurado, String contenedor, Map<TipoArchivo, long[]> totales,
                                    long configuracionesPendientes, long reportesPendientes) {
    }

    public record ResultadoSincronizacion(int configuraciones, int reportes, long pendientes) {
    }

    /**
     * Corre dentro de la transacción de la importación, así un fallo del repositorio la revierte. Sin repositorio
     * configurado no hace nada y la configuración queda pendiente de subir.
     */
    public Optional<ArchivoRepositorio> guardarConfiguracion(ConfiguracionDispositivo configuracion, String usuario) {
        if (!almacen.configurado()) {
            return Optional.empty();
        }
        String extension = switch (configuracion.formato()) {
            case CFG -> "cfg";
            default -> "txt";
        };
        byte[] contenido = configuracion.contenidoOriginal().getBytes(StandardCharsets.UTF_8);
        String nombre = String.format("CFG-%03d-v%d.%s", configuracion.id(), configuracion.version(), extension);
        // El prefijo del hash hace única la ruta aunque un id se repita (por ejemplo, después de restaurar un respaldo).
        String ruta = "configuraciones/org-" + configuracion.organizacionId() + "/dispositivo-"
                + configuracion.dispositivoId() + "/" + conHash(nombre, contenido);
        return Optional.of(subirYRegistrar(TipoArchivo.CONFIGURACION, configuracion.id(), configuracion.organizacionId(),
                ruta, nombre, TEXTO, contenido, usuario, "Fuente: " + recortar(configuracion.nombreFuente(), 180)));
    }

    /** Guarda el CSV del reporte tal como se generó (misma transacción que el reporte). */
    public Optional<ArchivoRepositorio> guardarReporte(ReporteAuditoria reporte, String usuario) {
        if (!almacen.configurado()) {
            return Optional.empty();
        }
        String nombre = String.format("REP-%03d.csv", reporte.id());
        byte[] contenido = ExportadorCsv.exportar(reporte).getBytes(StandardCharsets.UTF_8);
        String ruta = "reportes/" + (reporte.organizacionId() == null ? "todas" : "org-" + reporte.organizacionId())
                + "/" + conHash(nombre, contenido);
        return Optional.of(subirYRegistrar(TipoArchivo.REPORTE, reporte.id(), reporte.organizacionId(), ruta, nombre,
                CSV, contenido, usuario, recortar(reporte.titulo(), 200)));
    }

    /** Sube un archivo y lo registra en el catálogo (en la transacción en curso o en una propia). */
    ArchivoRepositorio subirYRegistrar(TipoArchivo tipo, Long entidadId, Long organizacionId, String ruta, String nombre,
                                       String tipoContenido, byte[] contenido, String usuario, String detalle) {
        return registro.guardar(subir(tipo, entidadId, organizacionId, ruta, nombre, tipoContenido, contenido, usuario, detalle));
    }

    /**
     * Sube el archivo y devuelve el registro del catálogo SIN guardarlo (lo guarda quien llama). Lo usan los
     * respaldos, para que la subida quede fuera de la transacción de la base.
     */
    ArchivoRepositorio subir(TipoArchivo tipo, Long entidadId, Long organizacionId, String ruta, String nombre,
                             String tipoContenido, byte[] contenido, String usuario, String detalle) {
        String hash = sha256(contenido);
        Map<String, String> metadatos = new LinkedHashMap<>();
        metadatos.put("sha256", hash);
        metadatos.put("tipo", tipo.name());
        if (entidadId != null) {
            metadatos.put("entidad", String.valueOf(entidadId));
        }
        try {
            almacen.subir(ruta, contenido, tipoContenido, metadatos);
        } catch (AlmacenamientoException exception) {
            // Ya existe (se subió y falló el registro en la base): si el contenido es igual se registra; si no, no
            // se reemplaza.
            if (exception.estadoHttp() != 409) {
                throw exception;
            }
            if (!sha256(almacen.descargar(ruta)).equals(hash)) {
                throw new AlmacenamientoException("Ya existe en el repositorio un archivo distinto con el nombre "
                        + nombre + ": no se reemplaza.", 409, exception);
            }
        }
        return new ArchivoRepositorio(null, tipo, entidadId, organizacionId, ruta, nombre,
                tipoContenido, contenido.length, hash, Instant.now(), recortar(usuarioSeguro(usuario), 120),
                detalle == null ? null : recortar(detalle, 500));
    }

    /** CFG-001-v1.cfg → CFG-001-v1-3fa2b9c41d07.cfg */
    private static String conHash(String nombre, byte[] contenido) {
        int punto = nombre.lastIndexOf('.');
        return nombre.substring(0, punto) + "-" + sha256(contenido).substring(0, 12) + nombre.substring(punto);
    }

    /**
     * Devuelve el archivo de una configuración o un reporte después de comprobar su hash.
     *
     * @throws NoSuchElementException si todavía no tiene archivo (pendiente de sincronizar)
     * @throws ArchivoAlteradoException si el contenido no coincide con el hash registrado
     */
    public ArchivoVerificado descargar(TipoArchivo tipo, Long entidadId) {
        ArchivoRepositorio archivo = archivoRepository.buscarPorEntidad(tipo, entidadId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Todavía no tiene archivo en el repositorio (pendiente de subir desde el panel de administración)."));
        return verificar(archivo);
    }

    public ArchivoVerificado descargarPorId(Long archivoId, TipoArchivo tipoEsperado) {
        ArchivoRepositorio archivo = archivoRepository.buscarPorId(archivoId)
                .filter(encontrado -> encontrado.tipo() == tipoEsperado)
                .orElseThrow(() -> new NoSuchElementException("El archivo solicitado no existe."));
        return verificar(archivo);
    }

    public Optional<ArchivoRepositorio> buscar(TipoArchivo tipo, Long entidadId) {
        return archivoRepository.buscarPorEntidad(tipo, entidadId);
    }

    private ArchivoVerificado verificar(ArchivoRepositorio archivo) {
        byte[] contenido = almacen.descargar(archivo.ruta());
        if (!sha256(contenido).equals(archivo.sha256())) {
            throw new ArchivoAlteradoException(archivo);
        }
        return new ArchivoVerificado(archivo, contenido);
    }

    public EstadoRepositorio estado() {
        return new EstadoRepositorio(almacen.configurado(), almacen.contenedor(), archivoRepository.totalesPorTipo(),
                archivoRepository.contarConfiguracionesSinArchivo(), archivoRepository.contarReportesSinArchivo());
    }

    /**
     * Sube los archivos de configuraciones y reportes que todavía no tienen uno, hasta
     * {@value #LOTE_SINCRONIZACION} de cada tipo. Cada archivo se registra en su propia transacción para que un
     * corte no pierda lo ya subido.
     */
    public ResultadoSincronizacion sincronizarPendientes(String usuario) {
        if (!almacen.configurado()) {
            throw new AlmacenamientoException("El repositorio de archivos no está configurado.", 503, null);
        }
        int configuraciones = 0;
        for (Long id : archivoRepository.configuracionesSinArchivo(LOTE_SINCRONIZACION)) {
            Optional<ConfiguracionDispositivo> configuracion = configuracionRepository.buscarPorId(id);
            if (configuracion.isPresent() && guardarConfiguracion(configuracion.get(), usuario).isPresent()) {
                configuraciones++;
            }
        }
        int reportes = 0;
        for (Long id : archivoRepository.reportesSinArchivo(LOTE_SINCRONIZACION)) {
            Optional<ReporteAuditoria> reporte = reporteRepository.buscarPorId(id);
            if (reporte.isPresent() && guardarReporte(reporte.get(), usuario).isPresent()) {
                reportes++;
            }
        }
        if (configuraciones + reportes > 0) {
            registro.registrar(new Transaccion("ARCHIVO", null, OperacionTransaccion.ALTA, null, null,
                    Map.of("configuraciones", configuraciones, "reportes", reportes), usuarioSeguro(usuario),
                    "Archivos pendientes subidos al repositorio: " + configuraciones + " configuraciones y "
                            + reportes + " reportes"));
        }
        long pendientes = archivoRepository.contarConfiguracionesSinArchivo() + archivoRepository.contarReportesSinArchivo();
        return new ResultadoSincronizacion(configuraciones, reportes, pendientes);
    }

    public List<ArchivoRepositorio> listar(TipoArchivo tipo, int limite) {
        return archivoRepository.listar(tipo, limite);
    }

    public static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }

    private static String usuarioSeguro(String usuario) {
        return usuario == null || usuario.isBlank() ? "SISTEMA_O_ANONIMO" : usuario;
    }

    private static String recortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    /** El archivo del repositorio no coincide con el hash registrado: posible alteración. */
    public static class ArchivoAlteradoException extends RuntimeException {
        public ArchivoAlteradoException(ArchivoRepositorio archivo) {
            super("El archivo " + archivo.nombre() + " no coincide con el hash SHA-256 registrado al guardarlo: "
                    + "pudo haber sido alterado. No se entrega; revisá la bitácora y los respaldos.");
        }
    }
}
