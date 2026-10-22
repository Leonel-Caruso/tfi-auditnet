package ar.edu.uai.tfi.auditcore.application.plan;

/** Operación que el plan de la organización no permite (límite alcanzado o período de prueba vencido): 409. */
public class RestriccionPlanException extends IllegalStateException {

    public RestriccionPlanException(String mensaje) {
        super(mensaje);
    }
}
