package ar.edu.uai.tfi.management.domain.model;

/**
 * Eventos de seguridad que registra la bitácora de auditoría de sistema
 * (Puntos Mandatorios 2026: inicios de sesión, creación de usuarios y asignación de privilegios).
 */
public enum TipoEventoSistema {
    LOGIN_EXITOSO,
    LOGIN_RECHAZADO,
    USUARIO_CREADO,
    ROLES_ASIGNADOS,
    ROL_CREADO
}
