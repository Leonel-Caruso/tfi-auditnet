package ar.edu.uai.tfi.management.domain.model;

/** Eventos de seguridad que registra la bitácora de auditoría de sistema. */
public enum TipoEventoSistema {
    LOGIN_EXITOSO,
    LOGIN_RECHAZADO,
    USUARIO_CREADO,
    ROLES_ASIGNADOS,
    ROL_CREADO,
    // Gestión de cuentas y sesiones
    USUARIO_MODIFICADO,
    USUARIO_DESHABILITADO,
    USUARIO_HABILITADO,
    CONTRASENA_CAMBIADA,
    CONTRASENA_RESTABLECIDA,
    CUENTA_BLOQUEADA,
    CUENTA_DESBLOQUEADA,
    ROL_MODIFICADO
}
