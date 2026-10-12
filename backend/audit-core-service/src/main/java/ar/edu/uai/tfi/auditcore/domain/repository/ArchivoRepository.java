package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catálogo del repositorio de archivos. Solo inserción: un archivo registrado no se modifica ni se borra. */
public interface ArchivoRepository {

    ArchivoRepositorio guardar(ArchivoRepositorio archivo);

    Optional<ArchivoRepositorio> buscarPorId(Long id);

    Optional<ArchivoRepositorio> buscarPorEntidad(TipoArchivo tipo, Long entidadId);

    /** Archivos de un tipo, del más nuevo al más viejo. */
    List<ArchivoRepositorio> listar(TipoArchivo tipo, int limite);

    /** Cantidad de archivos y bytes totales por tipo. */
    Map<TipoArchivo, long[]> totalesPorTipo();

    /** Configuraciones importadas que todavía no tienen su archivo en el repositorio. */
    List<Long> configuracionesSinArchivo(int limite);

    /** Reportes generados que todavía no tienen su archivo en el repositorio. */
    List<Long> reportesSinArchivo(int limite);

    long contarConfiguracionesSinArchivo();

    long contarReportesSinArchivo();
}
