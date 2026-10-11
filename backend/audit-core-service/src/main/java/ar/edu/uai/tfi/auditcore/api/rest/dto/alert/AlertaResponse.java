package ar.edu.uai.tfi.auditcore.api.rest.dto.alert;

import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;

import java.time.Instant;

/** Alerta crítica con los datos del dispositivo afectado y de la regla incumplida, para listar y filtrar. */
public record AlertaResponse(
        Long id,
        Long hallazgoId,
        Long auditoriaId,
        Long dispositivoId,
        Long organizacionId,
        String severidad,
        String estado,
        Instant fechaGeneracion,
        String perfilAsignado,
        String comentario,
        Instant fechaEstado,
        String usuarioEstado,
        String dispositivoIdentificador,
        String dispositivoNombre,
        Long sedeId,
        String criticidadDispositivo,
        String codigoRegla,
        String nombreRegla,
        String estadoHallazgo
) {

    public static AlertaResponse de(AlertaCritica alerta, DispositivoRed dispositivo, HallazgoAuditoria hallazgo) {
        return new AlertaResponse(
                alerta.id(),
                alerta.hallazgoId(),
                alerta.auditoriaId(),
                alerta.dispositivoId(),
                alerta.organizacionId(),
                alerta.severidad().name(),
                alerta.estado().name(),
                alerta.fechaGeneracion(),
                alerta.perfilAsignado() == null ? null : alerta.perfilAsignado().name(),
                alerta.comentario(),
                alerta.fechaEstado(),
                alerta.usuarioEstado(),
                dispositivo == null ? null : dispositivo.identificador(),
                dispositivo == null ? null : dispositivo.nombre(),
                dispositivo == null ? null : dispositivo.sedeId(),
                dispositivo == null || dispositivo.criticidad() == null ? null : dispositivo.criticidad().name(),
                hallazgo == null ? null : hallazgo.codigoRegla(),
                hallazgo == null ? null : hallazgo.nombreRegla(),
                hallazgo == null ? null : hallazgo.estado().name()
        );
    }
}
