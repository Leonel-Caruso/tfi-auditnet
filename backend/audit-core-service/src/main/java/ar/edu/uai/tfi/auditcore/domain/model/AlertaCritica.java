package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Alerta interna generada por un hallazgo CRITICA o ALTA. Conserva el vínculo con el hallazgo y con la auditoría
 * que lo detectó. No se borra nunca.
 *
 * @param perfilAsignado perfil técnico al que se derivó (null si no se derivó)
 * @param comentario     comentario del último cambio de estado
 */
public record AlertaCritica(
        Long id,
        Long hallazgoId,
        Long auditoriaId,
        Long dispositivoId,
        Long organizacionId,
        SeveridadRegla severidad,
        EstadoAlerta estado,
        Instant fechaGeneracion,
        PerfilTecnico perfilAsignado,
        String comentario,
        Instant fechaEstado,
        String usuarioEstado
) {

    public static boolean corresponde(SeveridadRegla severidad) {
        return severidad == SeveridadRegla.CRITICA || severidad == SeveridadRegla.ALTA;
    }
}
