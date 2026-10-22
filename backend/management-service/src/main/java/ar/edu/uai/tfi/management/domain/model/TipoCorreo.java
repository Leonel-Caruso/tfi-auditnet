package ar.edu.uai.tfi.management.domain.model;

/**
 * Tipos de aviso por correo. audit-core-service guarda los suyos con el mismo nombre en la bandeja de salida
 * (ALERTAS_CRITICAS, VENCIMIENTOS y RESUMEN_MENSUAL).
 */
public enum TipoCorreo {
    RECUPERACION_CONTRASENA,
    ALTA_USUARIO,
    SOLICITUD_PLAN,
    SOLICITUD_PLAN_RESUELTA,
    ALERTAS_CRITICAS,
    VENCIMIENTOS,
    RESUMEN_MENSUAL
}
