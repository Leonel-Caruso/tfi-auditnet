package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.TipoCorreo;

import java.util.Map;

/** Guarda un aviso en la bandeja de salida, dentro de la transacción del evento que lo origina. */
public interface BandejaCorreo {

    void encolar(TipoCorreo tipo, Long organizacionId, String destinatario, String nombre, Map<String, Object> datos);
}
