package ar.edu.uai.tfi.management.api.rest.dto.user;

import java.util.List;

/** Fila de la matriz de permisos: módulo, acción y perfiles habilitados (en el orden de la tesis). */
public record PermisoResponse(String modulo, String accion, List<String> perfiles) {
}
