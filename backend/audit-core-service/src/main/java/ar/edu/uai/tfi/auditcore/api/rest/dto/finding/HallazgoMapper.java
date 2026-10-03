package ar.edu.uai.tfi.auditcore.api.rest.dto.finding;

import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeguimientoHallazgo;

/** Conversión de hallazgos a respuestas REST, compartida por /api/findings y /api/audits. */
public final class HallazgoMapper {

    private HallazgoMapper() {
    }

    public static HallazgoResponse aResponse(HallazgoAuditoria hallazgo, CriticidadDispositivo criticidad) {
        return new HallazgoResponse(
                hallazgo.id(),
                hallazgo.auditoriaId(),
                hallazgo.reglaId(),
                hallazgo.dispositivoId(),
                hallazgo.baselineId(),
                hallazgo.organizacionId(),
                hallazgo.codigoRegla(),
                hallazgo.nombreRegla(),
                hallazgo.tipoRegla().name(),
                hallazgo.patron(),
                hallazgo.severidad().name(),
                hallazgo.evidencia(),
                hallazgo.recomendacion(),
                hallazgo.impacto(),
                hallazgo.estado().name(),
                hallazgo.fechaDeteccion(),
                hallazgo.fechaEstado(),
                hallazgo.usuarioEstado(),
                criticidad == null ? null : criticidad.name()
        );
    }

    public static SeguimientoHallazgoResponse aResponse(SeguimientoHallazgo seguimiento) {
        return new SeguimientoHallazgoResponse(
                seguimiento.id(),
                seguimiento.hallazgoId(),
                seguimiento.estadoAnterior().name(),
                seguimiento.estadoNuevo().name(),
                seguimiento.comentario(),
                seguimiento.usuario(),
                seguimiento.fecha()
        );
    }
}
