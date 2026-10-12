package ar.edu.uai.tfi.auditcore.application.dashboard;

import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.NivelRiesgo;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;

import java.util.Collection;
import java.util.Map;

/**
 * Nivel general de riesgo. Se aplica igual a una organización, una sede o un dispositivo, tomando sus hallazgos
 * pendientes y sus alertas vigentes:
 * <ol>
 *   <li>SIN_DATOS: nunca se auditó.</li>
 *   <li>ALTO: hay un hallazgo CRITICA pendiente, o una alerta vigente en un dispositivo de criticidad ALTA o
 *       CRITICA.</li>
 *   <li>MEDIO: hay un hallazgo ALTA pendiente.</li>
 *   <li>BAJO: solo quedan hallazgos MEDIA o BAJA pendientes, o ninguno.</li>
 * </ol>
 */
public final class EvaluadorRiesgo {

    private EvaluadorRiesgo() {
    }

    /**
     * @param auditado        true si hay al menos una auditoría en el alcance
     * @param pendientes      hallazgos ABIERTO o EN_REVISION del alcance
     * @param alertasVigentes alertas ACTIVA o EN_ATENCION del alcance
     * @param dispositivos    dispositivos por id, para conocer la criticidad del afectado por cada alerta
     */
    public static NivelRiesgo evaluar(boolean auditado,
                                      Collection<HallazgoAuditoria> pendientes,
                                      Collection<AlertaCritica> alertasVigentes,
                                      Map<Long, DispositivoRed> dispositivos) {
        if (!auditado) {
            return NivelRiesgo.SIN_DATOS;
        }
        boolean criticoPendiente = pendientes.stream().anyMatch(h -> h.severidad() == SeveridadRegla.CRITICA);
        boolean alertaEnEquipoCritico = alertasVigentes.stream()
                .map(alerta -> dispositivos.get(alerta.dispositivoId()))
                .anyMatch(dispositivo -> dispositivo != null && esCritico(dispositivo.criticidad()));
        if (criticoPendiente || alertaEnEquipoCritico) {
            return NivelRiesgo.ALTO;
        }
        if (pendientes.stream().anyMatch(h -> h.severidad() == SeveridadRegla.ALTA)) {
            return NivelRiesgo.MEDIO;
        }
        return NivelRiesgo.BAJO;
    }

    private static boolean esCritico(CriticidadDispositivo criticidad) {
        return criticidad == CriticidadDispositivo.ALTA || criticidad == CriticidadDispositivo.CRITICA;
    }
}
