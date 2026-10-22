package ar.edu.uai.tfi.auditcore.support;

import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.*;
import ar.edu.uai.tfi.auditcore.application.plan.ControlPlan;
import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort.AlmacenamientoException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Repositorios en memoria y datos de ejemplo para las pruebas de alertas y tablero. */
public final class EnMemoria {

    private EnMemoria() {
    }

    /** Control del plan para organizaciones sin plan cargado: no limita nada. */
    public static ControlPlan planSinLimites() {
        return new ControlPlan((organizacion, bloquear) -> Optional.empty(), new Dispositivos());
    }

    public static DispositivoRed dispositivo(Long id, Long organizacionId, Long sedeId, CriticidadDispositivo criticidad) {
        return new DispositivoRed(id, "Equipo " + id, "EQ-" + id, 1L, "Cisco", organizacionId, sedeId, criticidad,
                EstadoDispositivo.ACTIVO);
    }

    public static HallazgoAuditoria hallazgo(Long id, Long auditoriaId, Long dispositivoId, Long organizacionId,
                                             SeveridadRegla severidad, EstadoHallazgo estado, String fecha) {
        return new HallazgoAuditoria(id, auditoriaId, id, dispositivoId, 1L, organizacionId, "REG-" + id, "Regla " + id,
                TipoReglaConfiguracion.DEBE_CONTENER, "patron", severidad, "evidencia", "recomendación", "impacto",
                estado, Instant.parse(fecha), null, null);
    }

    public static AuditoriaConfiguracion auditoria(Long id, Long dispositivoId, Long organizacionId, String fecha,
                                                   int total, int cumplidas) {
        return new AuditoriaConfiguracion(id, id, dispositivoId, 1L, organizacionId, Instant.parse(fecha), "admin",
                total, cumplidas, total - cumplidas, "ALTA", cumplidas == total ? "CUMPLE" : "CON_HALLAZGOS",
                EstadoAuditoria.FINALIZADA);
    }

    public static final class Alertas implements AlertaRepository {
        public final List<AlertaCritica> items = new ArrayList<>();
        private long secuencia = 100;

        @Override public AlertaCritica guardar(AlertaCritica a) {
            AlertaCritica guardada = new AlertaCritica(a.id() == null ? ++secuencia : a.id(), a.hallazgoId(),
                    a.auditoriaId(), a.dispositivoId(), a.organizacionId(), a.severidad(), a.estado(),
                    a.fechaGeneracion(), a.perfilAsignado(), a.comentario(), a.fechaEstado(), a.usuarioEstado());
            items.add(guardada);
            return guardada;
        }
        @Override public Optional<AlertaCritica> buscarPorId(Long id) { return items.stream().filter(a -> a.id().equals(id)).findFirst(); }
        @Override public Optional<AlertaCritica> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(a -> a.organizacionId().equals(org)); }
        @Override public Optional<AlertaCritica> buscarPorHallazgo(Long id) { return items.stream().filter(a -> a.hallazgoId().equals(id)).findFirst(); }
        @Override public List<AlertaCritica> buscar(FiltroAlertas f) {
            return items.stream()
                    .filter(a -> f.organizacionId() == null || a.organizacionId().equals(f.organizacionId()))
                    .filter(a -> f.estado() != null ? a.estado() == f.estado() : !f.soloVigentes() || a.estado().vigente())
                    .filter(a -> f.severidad() == null || a.severidad() == f.severidad())
                    .filter(a -> f.dispositivoId() == null || a.dispositivoId().equals(f.dispositivoId()))
                    .toList();
        }
        @Override public AlertaCritica actualizarEstado(Long id, EstadoAlerta esperado, EstadoAlerta estado,
                                                        PerfilTecnico perfil, String comentario, String usuario,
                                                        Instant fecha) {
            AlertaCritica a = buscarPorId(id).orElseThrow();
            if (esperado != null && a.estado() != esperado) {
                throw new IllegalStateException("Otro usuario cambió la alerta a " + a.estado() + " mientras tanto.");
            }
            AlertaCritica nueva = new AlertaCritica(a.id(), a.hallazgoId(), a.auditoriaId(), a.dispositivoId(),
                    a.organizacionId(), a.severidad(), estado, a.fechaGeneracion(), perfil, comentario, fecha, usuario);
            items.replaceAll(existente -> existente.id().equals(id) ? nueva : existente);
            return nueva;
        }
    }

    public static final class Hallazgos implements HallazgoRepository {
        public final List<HallazgoAuditoria> items = new ArrayList<>();
        private long secuencia = 1000;

        @Override public HallazgoAuditoria guardar(HallazgoAuditoria h) {
            HallazgoAuditoria guardado = h.id() != null ? h : new HallazgoAuditoria(++secuencia, h.auditoriaId(), h.reglaId(),
                    h.dispositivoId(), h.baselineId(), h.organizacionId(), h.codigoRegla(), h.nombreRegla(), h.tipoRegla(),
                    h.patron(), h.severidad(), h.evidencia(), h.recomendacion(), h.impacto(), h.estado(),
                    h.fechaDeteccion(), h.fechaEstado(), h.usuarioEstado());
            items.add(guardado);
            return guardado;
        }
        @Override public List<HallazgoAuditoria> listar() { return List.copyOf(items); }
        @Override public List<HallazgoAuditoria> listarPorOrganizacion(Long org) { return items.stream().filter(h -> h.organizacionId().equals(org)).toList(); }
        @Override public List<HallazgoAuditoria> listarPorAuditoria(Long id) { return items.stream().filter(h -> h.auditoriaId().equals(id)).toList(); }
        @Override public Optional<HallazgoAuditoria> buscarPorId(Long id) { return items.stream().filter(h -> h.id().equals(id)).findFirst(); }
        @Override public Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(h -> h.organizacionId().equals(org)); }
        @Override public List<HallazgoAuditoria> buscar(FiltroHallazgos f) {
            return items.stream()
                    .filter(h -> f.organizacionId() == null || h.organizacionId().equals(f.organizacionId()))
                    .filter(h -> f.dispositivoId() == null || h.dispositivoId().equals(f.dispositivoId()))
                    .filter(h -> f.estado() == null || h.estado() == f.estado())
                    .toList();
        }
        @Override public HallazgoAuditoria actualizarEstado(Long id, EstadoHallazgo esperado, EstadoHallazgo estado,
                                                            String usuario, Instant fecha) {
            HallazgoAuditoria h = buscarPorId(id).orElseThrow();
            if (h.estado() != esperado) {
                throw new IllegalStateException("Otro usuario cambió el hallazgo a " + h.estado() + " mientras tanto.");
            }
            HallazgoAuditoria nuevo = new HallazgoAuditoria(h.id(), h.auditoriaId(), h.reglaId(), h.dispositivoId(),
                    h.baselineId(), h.organizacionId(), h.codigoRegla(), h.nombreRegla(), h.tipoRegla(), h.patron(),
                    h.severidad(), h.evidencia(), h.recomendacion(), h.impacto(), estado, h.fechaDeteccion(), fecha, usuario);
            items.replaceAll(existente -> existente.id().equals(id) ? nuevo : existente);
            return nuevo;
        }
    }

    public static final class Dispositivos implements DispositivoRepository {
        public final List<DispositivoRed> items = new ArrayList<>();

        @Override public DispositivoRed guardar(DispositivoRed d) { items.add(d); return d; }
        @Override public DispositivoRed actualizar(DispositivoRed d) { return d; }
        @Override public List<DispositivoRed> listar() { return List.copyOf(items); }
        @Override public List<DispositivoRed> listarPorOrganizacion(Long org) { return items.stream().filter(d -> d.organizacionId().equals(org)).toList(); }
        @Override public Optional<DispositivoRed> buscarPorId(Long id) { return items.stream().filter(d -> d.id().equals(id)).findFirst(); }
        @Override public Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(d -> d.organizacionId().equals(org)); }
        @Override public boolean existeIdentificadorEnOrganizacion(String i, Long org, Long excluir) { return false; }
    }

    public static final class Auditorias implements AuditoriaRepository {
        public final List<AuditoriaConfiguracion> items = new ArrayList<>();
        private long secuencia = 500;

        @Override public AuditoriaConfiguracion guardar(AuditoriaConfiguracion a) {
            AuditoriaConfiguracion guardada = a.id() != null ? a : new AuditoriaConfiguracion(++secuencia, a.configuracionId(),
                    a.dispositivoId(), a.baselineId(), a.organizacionId(), a.fechaEjecucion(), a.ejecutadoPor(), a.totalReglas(),
                    a.reglasCumplidas(), a.totalHallazgos(), a.severidadMaxima(), a.resultado(), a.estado());
            items.add(guardada);
            return guardada;
        }
        @Override public List<AuditoriaConfiguracion> listarHistorial() { return List.copyOf(items); }
        @Override public List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long org) { return buscarHistorial(new FiltroAuditorias(org, null, null, null, null, null, null, null)); }
        @Override public Optional<AuditoriaConfiguracion> buscarPorId(Long id) { return items.stream().filter(a -> a.id().equals(id)).findFirst(); }
        @Override public Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(a -> a.organizacionId().equals(org)); }
        @Override public List<AuditoriaConfiguracion> buscarHistorial(FiltroAuditorias f) {
            return items.stream()
                    .filter(a -> f.organizacionId() == null || a.organizacionId().equals(f.organizacionId()))
                    .filter(a -> f.dispositivoId() == null || a.dispositivoId().equals(f.dispositivoId()))
                    .filter(a -> f.desde() == null || !a.fechaEjecucion().isBefore(f.desde()))
                    .filter(a -> f.hasta() == null || !a.fechaEjecucion().isAfter(f.hasta()))
                    .toList();
        }
        @Override public long contarPorBaseline(Long baselineId) { return 0; }
        @Override public Map<Long, Long> contarPorBaselines() { return Map.of(); }
    }

    public static final class Organizaciones implements OrganizacionRepository {
        public final Set<Long> ids = new HashSet<>();

        @Override public boolean existe(Long id) { return ids.contains(id); }
    }

    public static final class Seguimientos implements SeguimientoHallazgoRepository {
        public final List<SeguimientoHallazgo> items = new ArrayList<>();

        @Override public SeguimientoHallazgo guardar(SeguimientoHallazgo s) { items.add(s); return s; }
        @Override public List<SeguimientoHallazgo> listarPorHallazgo(Long id) { return items.stream().filter(s -> s.hallazgoId().equals(id)).toList(); }
    }

    public static final class Configuraciones implements ConfiguracionRepository {
        public final List<ConfiguracionDispositivo> items = new ArrayList<>();

        @Override public ConfiguracionDispositivo guardar(ConfiguracionDispositivo c) { items.add(c); return c; }
        @Override public List<ConfiguracionDispositivo> listarAuditables() { return List.copyOf(items); }
        @Override public List<ConfiguracionDispositivo> listarAuditablesPorOrganizacion(Long org) { return items.stream().filter(c -> c.organizacionId().equals(org)).toList(); }
        @Override public Optional<ConfiguracionDispositivo> buscarPorId(Long id) { return items.stream().filter(c -> c.id().equals(id)).findFirst(); }
        @Override public Optional<ConfiguracionDispositivo> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(c -> c.organizacionId().equals(org)); }
        @Override public int siguienteVersionParaDispositivo(Long id) { return items.size() + 1; }
    }

    public static final class Baselines implements BaselineRepository {
        public final List<BaselineConfiguracion> items = new ArrayList<>();

        @Override public BaselineConfiguracion guardar(BaselineConfiguracion b) { items.add(b); return b; }
        @Override public BaselineConfiguracion actualizar(BaselineConfiguracion b) { return b; }
        @Override public List<BaselineConfiguracion> listar() { return List.copyOf(items); }
        @Override public List<BaselineConfiguracion> listarPorOrganizacion(Long org) { return items.stream().filter(b -> b.organizacionId().equals(org)).toList(); }
        @Override public Optional<BaselineConfiguracion> buscarPorId(Long id) { return items.stream().filter(b -> b.id().equals(id)).findFirst(); }
        @Override public Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id).filter(b -> b.organizacionId().equals(org)); }
        @Override public boolean existeNombreEnOrganizacion(String nombre, Long org) { return false; }
    }

    public static final class Reglas implements ReglaBaselineRepository {
        public final List<ReglaBaseline> items = new ArrayList<>();

        @Override public ReglaBaseline guardar(ReglaBaseline r) { items.add(r); return r; }
        @Override public ReglaBaseline actualizar(ReglaBaseline r) { return r; }
        @Override public List<ReglaBaseline> listar() { return List.copyOf(items); }
        @Override public List<ReglaBaseline> listarPorOrganizacion(Long org) { return List.copyOf(items); }
        @Override public List<ReglaBaseline> listarPorBaseline(Long id) { return items.stream().filter(r -> r.baselineId().equals(id)).toList(); }
        @Override public List<ReglaBaseline> listarPorBaselineYOrganizacion(Long id, Long org) { return listarPorBaseline(id); }
        @Override public Optional<ReglaBaseline> buscarPorId(Long id) { return items.stream().filter(r -> r.id().equals(id)).findFirst(); }
        @Override public Optional<ReglaBaseline> buscarPorIdYOrganizacion(Long id, Long org) { return buscarPorId(id); }
        @Override public boolean existeCodigoEnBaseline(String codigo, Long id) { return false; }
        @Override public long contarPorBaseline(Long id) { return listarPorBaseline(id).size(); }
    }

    public static final class Evaluaciones implements EvaluacionReglaRepository {
        public final List<ResultadoReglaAuditoria> items = new ArrayList<>();

        @Override public ResultadoReglaAuditoria guardar(ResultadoReglaAuditoria e) { items.add(e); return e; }
        @Override public List<ResultadoReglaAuditoria> listarPorAuditoria(Long id) { return items.stream().filter(e -> e.auditoriaId().equals(id)).toList(); }
    }

    public static final class Reportes implements ReporteRepository {
        public final List<ReporteAuditoria> items = new ArrayList<>();
        public final java.util.Map<Long, List<Long>> auditoriasPorReporte = new java.util.HashMap<>();

        @Override public ReporteAuditoria guardar(ReporteAuditoria r, List<Long> auditoriaIds) {
            ReporteAuditoria guardado = new ReporteAuditoria((long) items.size() + 1, r.organizacionId(), r.titulo(), r.criterios(),
                    r.contenido(), r.totalAuditorias(), r.totalHallazgos(), r.generadoPor(), r.fechaGeneracion());
            items.add(guardado);
            auditoriasPorReporte.put(guardado.id(), List.copyOf(auditoriaIds));
            return guardado;
        }
        @Override public Optional<ReporteAuditoria> buscarPorId(Long id) { return items.stream().filter(r -> r.id().equals(id)).findFirst(); }
        @Override public List<ReporteAuditoria> listar(Long org) {
            return items.stream().filter(r -> org == null || org.equals(r.organizacionId()))
                    .sorted(java.util.Comparator.comparing(ReporteAuditoria::id).reversed()).toList();
        }
    }

    // Repositorio de archivos

    /** Catálogo en memoria. */
    public static final class Archivos implements ArchivoRepository {
        public final List<ArchivoRepositorio> items = new ArrayList<>();
        public final Configuraciones configuraciones;
        public final Reportes reportes;

        public Archivos(Configuraciones configuraciones, Reportes reportes) {
            this.configuraciones = configuraciones;
            this.reportes = reportes;
        }

        @Override public ArchivoRepositorio guardar(ArchivoRepositorio a) {
            if (items.stream().anyMatch(x -> x.ruta().equals(a.ruta()))) throw new IllegalStateException("ruta duplicada");
            ArchivoRepositorio guardado = new ArchivoRepositorio((long) items.size() + 1, a.tipo(), a.entidadId(), a.organizacionId(),
                    a.ruta(), a.nombre(), a.tipoContenido(), a.tamanioBytes(), a.sha256(), a.fecha(), a.usuario(), a.detalle());
            items.add(guardado);
            return guardado;
        }
        @Override public Optional<ArchivoRepositorio> buscarPorId(Long id) { return items.stream().filter(a -> a.id().equals(id)).findFirst(); }
        @Override public Optional<ArchivoRepositorio> buscarPorEntidad(TipoArchivo tipo, Long id) {
            return items.stream().filter(a -> a.tipo() == tipo && id.equals(a.entidadId())).findFirst();
        }
        @Override public List<ArchivoRepositorio> listar(TipoArchivo tipo, int limite) {
            return items.stream().filter(a -> a.tipo() == tipo).sorted(java.util.Comparator.comparing(ArchivoRepositorio::id).reversed())
                    .limit(limite).toList();
        }
        @Override public Map<TipoArchivo, long[]> totalesPorTipo() {
            Map<TipoArchivo, long[]> totales = new java.util.EnumMap<>(TipoArchivo.class);
            for (TipoArchivo t : TipoArchivo.values()) {
                totales.put(t, new long[]{items.stream().filter(a -> a.tipo() == t).count(),
                        items.stream().filter(a -> a.tipo() == t).mapToLong(ArchivoRepositorio::tamanioBytes).sum()});
            }
            return totales;
        }
        @Override public List<Long> configuracionesSinArchivo(int limite) {
            return configuraciones.items.stream().map(ConfiguracionDispositivo::id)
                    .filter(id -> buscarPorEntidad(TipoArchivo.CONFIGURACION, id).isEmpty()).limit(limite).toList();
        }
        @Override public List<Long> reportesSinArchivo(int limite) {
            return reportes.items.stream().map(ReporteAuditoria::id)
                    .filter(id -> buscarPorEntidad(TipoArchivo.REPORTE, id).isEmpty()).limit(limite).toList();
        }
        @Override public long contarConfiguracionesSinArchivo() { return configuracionesSinArchivo(Integer.MAX_VALUE).size(); }
        @Override public long contarReportesSinArchivo() { return reportesSinArchivo(Integer.MAX_VALUE).size(); }
    }

    /** Almacenamiento en memoria con las mismas reglas que Blob Storage (no reemplaza archivos). */
    public static final class Almacen implements ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort {
        public final Map<String, byte[]> archivos = new java.util.LinkedHashMap<>();
        public final Map<String, Map<String, String>> metadatos = new java.util.LinkedHashMap<>();
        public boolean configurado = true;
        public boolean caido;

        @Override public boolean configurado() { return configurado; }
        @Override public String contenedor() { return "auditnet-archivos"; }
        @Override public void subir(String ruta, byte[] contenido, String tipo, Map<String, String> meta) {
            if (caido) throw new AlmacenamientoException("El repositorio de archivos no responde (ConnectException).", 503, null);
            if (archivos.containsKey(ruta)) throw new AlmacenamientoException("No se pudo guardar el archivo en el repositorio (BlobAlreadyExists).", 409, null);
            archivos.put(ruta, contenido.clone());
            metadatos.put(ruta, Map.copyOf(meta));
        }
        @Override public byte[] descargar(String ruta) {
            if (caido) throw new AlmacenamientoException("El repositorio de archivos no responde (ConnectException).", 503, null);
            byte[] contenido = archivos.get(ruta);
            if (contenido == null) throw new AlmacenamientoException("No se pudo leer el archivo del repositorio (BlobNotFound).", 404, null);
            return contenido.clone();
        }
        @Override public void verificar() { if (caido) throw new AlmacenamientoException("caído", 503, null); }
    }
}
