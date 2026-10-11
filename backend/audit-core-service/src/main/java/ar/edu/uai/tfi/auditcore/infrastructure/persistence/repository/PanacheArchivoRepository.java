package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;
import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;
import ar.edu.uai.tfi.auditcore.domain.repository.ArchivoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.ArchivoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PanacheArchivoRepository implements PanacheRepository<ArchivoEntity>, ArchivoRepository {

    @Override
    public ArchivoRepositorio guardar(ArchivoRepositorio archivo) {
        ArchivoEntity entity = new ArchivoEntity();
        entity.tipo = archivo.tipo();
        entity.entidadId = archivo.entidadId();
        entity.organizacionId = archivo.organizacionId();
        entity.ruta = archivo.ruta();
        entity.nombre = archivo.nombre();
        entity.tipoContenido = archivo.tipoContenido();
        entity.tamanioBytes = archivo.tamanioBytes();
        entity.sha256 = archivo.sha256();
        entity.fecha = archivo.fecha();
        entity.usuario = archivo.usuario();
        entity.detalle = archivo.detalle();
        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public Optional<ArchivoRepositorio> buscarPorId(Long id) {
        return findByIdOptional(id).map(PanacheArchivoRepository::convertir);
    }

    @Override
    public Optional<ArchivoRepositorio> buscarPorEntidad(TipoArchivo tipo, Long entidadId) {
        return find("tipo = ?1 and entidadId = ?2", tipo, entidadId).firstResultOptional()
                .map(PanacheArchivoRepository::convertir);
    }

    @Override
    public List<ArchivoRepositorio> listar(TipoArchivo tipo, int limite) {
        return find("tipo = ?1 order by fecha desc, id desc", tipo).page(0, limite).list().stream()
                .map(PanacheArchivoRepository::convertir).toList();
    }

    @Override
    public Map<TipoArchivo, long[]> totalesPorTipo() {
        Map<TipoArchivo, long[]> totales = new EnumMap<>(TipoArchivo.class);
        for (TipoArchivo tipo : TipoArchivo.values()) {
            totales.put(tipo, new long[]{0, 0});
        }
        List<Object[]> filas = getEntityManager()
                .createQuery("select a.tipo, count(a), coalesce(sum(a.tamanioBytes), 0) from ArchivoEntity a group by a.tipo",
                        Object[].class)
                .getResultList();
        for (Object[] fila : filas) {
            totales.put((TipoArchivo) fila[0], new long[]{((Number) fila[1]).longValue(), ((Number) fila[2]).longValue()});
        }
        return totales;
    }

    @Override
    public List<Long> configuracionesSinArchivo(int limite) {
        return idsSinArchivo("configuraciones_dispositivo", "id_configuracion", TipoArchivo.CONFIGURACION, limite);
    }

    @Override
    public List<Long> reportesSinArchivo(int limite) {
        return idsSinArchivo("reportes_auditoria", "id_reporte", TipoArchivo.REPORTE, limite);
    }

    @Override
    public long contarConfiguracionesSinArchivo() {
        return contarSinArchivo("configuraciones_dispositivo", "id_configuracion", TipoArchivo.CONFIGURACION);
    }

    @Override
    public long contarReportesSinArchivo() {
        return contarSinArchivo("reportes_auditoria", "id_reporte", TipoArchivo.REPORTE);
    }

    // Tabla y columna son constantes de esta clase (no vienen del usuario).
    @SuppressWarnings("unchecked")
    private List<Long> idsSinArchivo(String tabla, String columna, TipoArchivo tipo, int limite) {
        List<Number> ids = getEntityManager().createNativeQuery(
                        "SELECT t." + columna + " FROM " + tabla + " t WHERE NOT EXISTS (SELECT 1 FROM archivos_repositorio a"
                                + " WHERE a.tipo = ?1 AND a.id_entidad = t." + columna + ") ORDER BY t." + columna + " LIMIT ?2")
                .setParameter(1, tipo.name())
                .setParameter(2, limite)
                .getResultList();
        return ids.stream().map(Number::longValue).toList();
    }

    private long contarSinArchivo(String tabla, String columna, TipoArchivo tipo) {
        Number total = (Number) getEntityManager().createNativeQuery(
                        "SELECT count(*) FROM " + tabla + " t WHERE NOT EXISTS (SELECT 1 FROM archivos_repositorio a"
                                + " WHERE a.tipo = ?1 AND a.id_entidad = t." + columna + ")")
                .setParameter(1, tipo.name())
                .getSingleResult();
        return total.longValue();
    }

    private static ArchivoRepositorio convertir(ArchivoEntity entity) {
        return new ArchivoRepositorio(entity.id, entity.tipo, entity.entidadId, entity.organizacionId, entity.ruta,
                entity.nombre, entity.tipoContenido, entity.tamanioBytes, entity.sha256, entity.fecha, entity.usuario,
                entity.detalle);
    }
}
