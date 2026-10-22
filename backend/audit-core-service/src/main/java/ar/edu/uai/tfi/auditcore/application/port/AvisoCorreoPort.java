package ar.edu.uai.tfi.auditcore.application.port;

import java.util.Map;

/**
 * Avisos por correo a los usuarios de una organización. Se guardan en la bandeja de salida común dentro de la
 * transacción en curso; management-service los arma y los envía.
 */
public interface AvisoCorreoPort {

    /** Cada tipo llega a ciertos perfiles y respeta la preferencia de aviso de cada usuario. */
    enum Tipo {
        ALERTAS_CRITICAS,
        VENCIMIENTOS,
        RESUMEN_MENSUAL
    }

    /** @return cantidad de destinatarios */
    int avisar(Tipo tipo, Long organizacionId, Map<String, Object> datos);
}
