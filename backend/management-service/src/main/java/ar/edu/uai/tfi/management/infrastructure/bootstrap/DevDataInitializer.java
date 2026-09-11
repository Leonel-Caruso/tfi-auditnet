package ar.edu.uai.tfi.management.infrastructure.bootstrap;

import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Set;

@ApplicationScoped
@UnlessBuildProfile("test")
public class DevDataInitializer {
    private static final Logger LOG = Logger.getLogger(DevDataInitializer.class);

    private final OrganizacionClienteRepository organizacionRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordPort passwordPort;

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
                              RolRepository rolRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordPort passwordPort) {
        this.organizacionRepository = organizacionRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordPort = passwordPort;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        crearRolesBase();
        OrganizacionCliente organizacion = obtenerOCrearOrganizacion();
        crearAdministradorSiNoExiste(organizacion.id());
    }

    private void crearRolesBase() {
        List<RolBase> roles = List.of(
                new RolBase("ADMINISTRADOR_SISTEMA", "Gestiona usuarios, roles, clientes y configuración general."),
                new RolBase("ANALISTA_RED", "Carga configuraciones, ejecuta auditorías y analiza hallazgos."),
                new RolBase("AUDITOR_TECNICO", "Consulta evidencia, reportes e historial de auditoría."),
                new RolBase("RESPONSABLE_GESTION_IT", "Consulta indicadores, reportes y alertas relevantes.")
        );

        for (RolBase rolBase : roles) {
            if (rolRepository.buscarPorNombre(rolBase.nombre()).isEmpty()) {
                rolRepository.guardar(new Rol(null, rolBase.nombre(), rolBase.descripcion(), EstadoRegistro.ACTIVO));
            }
        }
    }

    private OrganizacionCliente obtenerOCrearOrganizacion() {
        return organizacionRepository.buscarPorIdentificador(organizationIdentifier)
                .orElseGet(() -> organizacionRepository.guardar(new OrganizacionCliente(
                        null,
                        organizationIdentifier.trim().toUpperCase(),
                        organizationName.trim(),
                        EstadoRegistro.ACTIVO)));
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
    }

    private record RolBase(String nombre, String descripcion) {
    }
}
