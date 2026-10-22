package ar.edu.uai.tfi.management.application.plan;

/** Control que hacen otros servicios antes de sumar un usuario activo a una organización. */
public interface ControlPlan {

    /**
     * Rechaza con 409 si el período piloto venció o si la organización ya llegó al máximo de usuarios de su plan.
     * Bloquea la organización hasta el fin de la transacción para que dos altas simultáneas no superen el límite.
     */
    void verificarAltaUsuario(Long organizacionId);
}
