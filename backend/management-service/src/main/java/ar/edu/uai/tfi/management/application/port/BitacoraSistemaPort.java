package ar.edu.uai.tfi.management.application.port;

import ar.edu.uai.tfi.management.domain.model.EventoSistema;

/**
 * Puerto de la bitácora de auditoría de sistema (eventos de seguridad).
 * Los servicios informan qué ocurrió; el adaptador agrega fecha, IP, navegador
 * y correlación, y lo persiste.
 */
public interface BitacoraSistemaPort {

    void registrar(EventoSistema evento);
}
