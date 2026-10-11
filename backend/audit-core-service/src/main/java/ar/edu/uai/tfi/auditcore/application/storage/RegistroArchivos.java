package ar.edu.uai.tfi.auditcore.application.storage;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.ArchivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.function.Function;

/**
 * Registro en el catálogo de archivos, en transacciones cortas. Si ya hay una transacción en curso (importación de
 * una configuración, generación de un reporte) se suma a ella; si no (respaldos, archivos pendientes) abre una
 * propia. Así las subidas al almacenamiento, que pueden tardar, quedan fuera de las transacciones de la base.
 */
@ApplicationScoped
public class RegistroArchivos {

    private final ArchivoRepository archivoRepository;
    private final TrazabilidadPort trazabilidad;

    public RegistroArchivos(ArchivoRepository archivoRepository, TrazabilidadPort trazabilidad) {
        this.archivoRepository = archivoRepository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public ArchivoRepositorio guardar(ArchivoRepositorio archivo) {
        return archivoRepository.guardar(archivo);
    }

    /** Guarda el archivo y su registro en la bitácora de transacciones en la misma transacción. */
    @Transactional
    public ArchivoRepositorio guardar(ArchivoRepositorio archivo, Function<ArchivoRepositorio, Transaccion> registro) {
        ArchivoRepositorio guardado = archivoRepository.guardar(archivo);
        trazabilidad.registrar(registro.apply(guardado));
        return guardado;
    }

    @Transactional
    public void registrar(Transaccion transaccion) {
        trazabilidad.registrar(transaccion);
    }
}
