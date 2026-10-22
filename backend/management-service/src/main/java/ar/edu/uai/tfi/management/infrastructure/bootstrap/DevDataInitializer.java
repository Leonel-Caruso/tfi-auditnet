package ar.edu.uai.tfi.management.infrastructure.bootstrap;

import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Plan;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.PlanRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@UnlessBuildProfile("test")
public class DevDataInitializer {
    private static final Logger LOG = Logger.getLogger(DevDataInitializer.class);

    private final OrganizacionClienteRepository organizacionRepository;
    private final PlanRepository planRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordPort passwordPort;
    private final BitacoraSistemaPort bitacora;

    @ConfigProperty(name = "app.bootstrap.admin.email")
    String adminEmail;
    @ConfigProperty(name = "app.bootstrap.admin.username")
    String adminUsername;
    @ConfigProperty(name = "app.bootstrap.admin.password")
    String adminPassword;
    @ConfigProperty(name = "app.bootstrap.organization.identifier")
    String organizationIdentifier;
    @ConfigProperty(name = "app.bootstrap.organization.name")
    String organizationName;

    public DevDataInitializer(OrganizacionClienteRepository organizacionRepository,
                              PlanRepository planRepository,
                              RolRepository rolRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordPort passwordPort,
                              BitacoraSistemaPort bitacora) {
        this.organizacionRepository = organizacionRepository;
        this.planRepository = planRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordPort = passwordPort;
        this.bitacora = bitacora;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        crearRolesBase();
        OrganizacionCliente organizacion = obtenerOCrearOrganizacion();
        crearAdministradorSiNoExiste(organizacion.id());
    }

    private void crearRolesBase() {
        List<RolBase> roles = List.of(
                new RolBase("ADMINISTRADOR_SISTEMA", "Administra usuarios, organizaciones, sedes, bitácoras y la salud de la plataforma."),
                new RolBase("ADMINISTRADOR_ORGANIZACION", "Administra los usuarios, las sedes y los dispositivos de su organización y consulta su plan."),
                new RolBase("ANALISTA_RED", "Importa configuraciones, ejecuta auditorías, atiende hallazgos y deriva alertas."),
                new RolBase("AUDITOR_TECNICO", "Define baselines y reglas, revisa hallazgos y genera reportes de auditoría."),
                new RolBase("RESPONSABLE_GESTION_IT", "Consulta el tablero, las alertas y los reportes de su organización y deriva alertas.")
        );

        for (RolBase rolBase : roles) {
            if (rolRepository.buscarPorNombre(rolBase.nombre()).isEmpty()) {
                Rol creado = rolRepository.guardar(
                        new Rol(null, rolBase.nombre(), rolBase.descripcion(), EstadoRegistro.ACTIVO));
                bitacora.registrar(EventoSistema.exito(TipoEventoSistema.ROL_CREADO, null, null, null,
                        "rolId=" + creado.id() + ", nombre=" + creado.nombre() + ", origen=bootstrap"));
            }
        }
    }

    /** La organización de la plataforma empieza con el plan Inicial, como las existentes al migrar a V5. */
    private OrganizacionCliente obtenerOCrearOrganizacion() {
        return organizacionRepository.buscarPorIdentificador(organizationIdentifier)
                .orElseGet(() -> {
                    Plan inicial = planRepository.buscarPorCodigo("INICIAL")
                            .orElseThrow(() -> new IllegalStateException("Falta el plan INICIAL (migración V5)."));
                    return organizacionRepository.guardar(new OrganizacionCliente(
                            null,
                            organizationIdentifier.trim().toUpperCase(),
                            organizationName.trim(),
                            EstadoRegistro.ACTIVO), new PlanOrganizacion(null, inicial, Instant.now(), null, null, null));
                });
    }

    private void crearAdministradorSiNoExiste(Long organizacionId) {
        if (usuarioRepository.buscarPorEmail(adminEmail).isPresent()
                || usuarioRepository.buscarPorNombreUsuario(adminUsername).isPresent()) {
            LOG.info("Bootstrap: el usuario administrador ya existe; no se vuelve a crear.");
            return;
        }

        Set<String> roles = Set.of("ADMINISTRADOR_SISTEMA");
        Usuario admin = new Usuario(
                null,
                organizacionId,
                "Administrador TFI",
                adminEmail.trim().toLowerCase(),
                adminUsername.trim().toLowerCase(),
                passwordPort.hashear(adminPassword),
                EstadoRegistro.ACTIVO,
                roles);
        Usuario creado = usuarioRepository.guardar(admin, roles);
        LOG.infof("Bootstrap: administrador creado con id=%d y contraseña almacenada con hash BCrypt.", creado.id());
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.USUARIO_CREADO, null, creado.id(), organizacionId,
                "username=" + creado.nombreUsuario() + ", origen=bootstrap"));
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.ROLES_ASIGNADOS, null, creado.id(), organizacionId,
                "roles=" + String.join(",", roles) + ", origen=bootstrap"));
    }

    private record RolBase(String nombre, String descripcion) {
    }
}
