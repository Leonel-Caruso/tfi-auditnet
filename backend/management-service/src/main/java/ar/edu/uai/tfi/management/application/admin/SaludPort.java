package ar.edu.uai.tfi.management.application.admin;

import java.util.List;

/** Verifica los componentes de infraestructura del servicio (PostgreSQL y MongoDB). */
public interface SaludPort {

    List<ComponenteSalud> verificarComponentes();
}
