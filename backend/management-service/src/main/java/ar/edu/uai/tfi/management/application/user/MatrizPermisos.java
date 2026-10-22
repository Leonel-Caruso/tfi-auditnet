package ar.edu.uai.tfi.management.application.user;

import java.util.List;
import java.util.Set;

/**
 * Permisos por perfil tal como están aplicados con {@code @RolesAllowed} en los dos servicios; se muestra en el
 * panel de usuarios. Si cambia un permiso en el código, hay que actualizar esta tabla y {@code RolServiceTest}.
 */
public final class MatrizPermisos {

    public static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    public static final String ADMIN_ORGANIZACION = "ADMINISTRADOR_ORGANIZACION";
    public static final String ANALISTA = "ANALISTA_RED";
    public static final String AUDITOR = "AUDITOR_TECNICO";
    public static final String RESPONSABLE = "RESPONSABLE_GESTION_IT";
    public static final List<String> PERFILES = List.of(ADMIN, ADMIN_ORGANIZACION, ANALISTA, AUDITOR, RESPONSABLE);

    /** Perfiles de la operación diaria: los únicos que asigna el administrador de una organización. */
    public static final Set<String> PERFILES_OPERACION = Set.of(ANALISTA, AUDITOR, RESPONSABLE);

    private MatrizPermisos() {
    }

    /** Una fila: módulo, acción y perfiles habilitados. */
    public record Permiso(String modulo, String accion, Set<String> perfiles) {
    }

    private static final Set<String> TODOS = Set.copyOf(PERFILES);

    public static final List<Permiso> PERMISOS = List.of(
            new Permiso("Tablero y estado del cliente", "Consultar", TODOS),
            new Permiso("Dispositivos", "Consultar", TODOS),
            new Permiso("Dispositivos", "Registrar, modificar y deshabilitar", Set.of(ADMIN, ADMIN_ORGANIZACION)),
            new Permiso("Tipos de dispositivo", "Registrar", Set.of(ADMIN)),
            new Permiso("Baselines y reglas", "Consultar", TODOS),
            new Permiso("Baselines y reglas", "Crear, versionar y modificar", Set.of(ADMIN, AUDITOR)),
            new Permiso("Configuraciones", "Importar", Set.of(ADMIN, ANALISTA, AUDITOR)),
            new Permiso("Auditorías", "Ejecutar", Set.of(ADMIN, ANALISTA, AUDITOR)),
            new Permiso("Auditorías e historial", "Consultar", TODOS),
            new Permiso("Hallazgos", "Consultar", TODOS),
            new Permiso("Hallazgos", "Cambiar el estado de seguimiento", Set.of(ADMIN, ANALISTA, AUDITOR)),
            new Permiso("Alertas críticas", "Consultar", TODOS),
            new Permiso("Alertas críticas", "Derivar o devolver", Set.of(ADMIN, ANALISTA, RESPONSABLE)),
            new Permiso("Reportes", "Consultar y descargar", TODOS),
            new Permiso("Reportes", "Generar", Set.of(ADMIN, ANALISTA, AUDITOR)),
            new Permiso("Organizaciones y sedes", "Consultar la propia", TODOS),
            new Permiso("Organizaciones", "Registrar, modificar y deshabilitar", Set.of(ADMIN)),
            new Permiso("Sedes", "Registrar, modificar y deshabilitar", Set.of(ADMIN, ADMIN_ORGANIZACION)),
            new Permiso("Usuarios", "Gestionar administradores", Set.of(ADMIN)),
            new Permiso("Usuarios", "Gestionar perfiles de operación", Set.of(ADMIN, ADMIN_ORGANIZACION)),
            new Permiso("Plan", "Consultar el de la organización", TODOS),
            new Permiso("Plan", "Consultar las solicitudes", Set.of(ADMIN, ADMIN_ORGANIZACION, RESPONSABLE)),
            new Permiso("Plan", "Solicitar un cambio", Set.of(ADMIN_ORGANIZACION)),
            new Permiso("Planes", "Asignar, aprobar solicitudes y editar precios", Set.of(ADMIN)),
            new Permiso("Bitácoras", "Consultar", Set.of(ADMIN)),
            new Permiso("Panel de administración (salud de servicios)", "Consultar", Set.of(ADMIN)),
            new Permiso("Mi cuenta", "Cambiar la propia contraseña", TODOS)
    );
}
