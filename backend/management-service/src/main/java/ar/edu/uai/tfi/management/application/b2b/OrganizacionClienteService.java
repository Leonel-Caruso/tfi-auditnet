package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class OrganizacionClienteService {
    private final OrganizacionClienteRepository repository;
    private final TrazabilidadPort trazabilidad;
    private final UsuarioRepository usuarioRepository;
    Clock reloj = Clock.systemUTC();

    public OrganizacionClienteService(OrganizacionClienteRepository repository, TrazabilidadPort trazabilidad,
                                      UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
        this.usuarioRepository = usuarioRepository;
    }

    public List<OrganizacionCliente> listar() {
        return repository.listar();
    }

    public OrganizacionCliente buscarPorId(Long id) {
        if (id == null) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "El id de organización es obligatorio.");
        }
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionAplicacion(
                        ErrorAplicacion.NO_ENCONTRADO,
                        "La organización cliente no fue encontrada."));
    }

    @Transactional
    public OrganizacionCliente crear(String identificador, String razonSocial) {
        validarObligatorio(identificador, "identificador");
        validarObligatorio(razonSocial, "razonSocial");

        String identificadorNormalizado = identificador.trim().toUpperCase(Locale.ROOT);
        String razonSocialNormalizada = razonSocial.trim();

        if (repository.buscarPorIdentificador(identificadorNormalizado).isPresent()
                || repository.buscarPorRazonSocial(razonSocialNormalizada).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe una organización cliente con el mismo identificador o razón social.");
        }

        OrganizacionCliente creada = repository.guardar(new OrganizacionCliente(
                null,
                identificadorNormalizado,
                razonSocialNormalizada,
                EstadoRegistro.ACTIVO));

        trazabilidad.registrar(Transaccion.alta("ORGANIZACION", creada.id(), creada.id(), creada));
        return creada;
    }

    /** Modifica identificador y razón social; ambos siguen siendo únicos en la plataforma. */
    @Transactional
    public OrganizacionCliente actualizar(Long id, String identificador, String razonSocial) {
        OrganizacionCliente anterior = buscarPorId(id);
        validarObligatorio(identificador, "identificador");
        validarObligatorio(razonSocial, "razonSocial");
        String identificadorNormalizado = identificador.trim().toUpperCase(Locale.ROOT);
        String razonSocialNormalizada = razonSocial.trim();
        boolean duplicada = repository.buscarPorIdentificador(identificadorNormalizado)
                .filter(otra -> !otra.id().equals(id)).isPresent()
                || repository.buscarPorRazonSocial(razonSocialNormalizada)
                .filter(otra -> !otra.id().equals(id)).isPresent();
        if (duplicada) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe otra organización cliente con el mismo identificador o razón social.");
        }
        OrganizacionCliente actualizada = repository.actualizar(new OrganizacionCliente(id, identificadorNormalizado,
                razonSocialNormalizada, anterior.estado()));
        trazabilidad.registrar(new Transaccion("ORGANIZACION", id, OperacionTransaccion.MODIFICACION, id,
                anterior, actualizada, null, null));
        return actualizada;
    }

    /** Baja lógica: mientras está deshabilitada sus usuarios no pueden ingresar ni operar; los datos se conservan. */
    @Transactional
    public OrganizacionCliente cambiarEstado(Long id, String estado) {
        OrganizacionCliente anterior = buscarPorId(id);
        EstadoRegistro nuevo = parsearEstado(estado);
        if (anterior.estado() == nuevo) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "La organización ya está " + nuevo + ".");
        }
        OrganizacionCliente actualizada = repository.actualizar(new OrganizacionCliente(id, anterior.identificador(),
                anterior.razonSocial(), nuevo));
        if (nuevo == EstadoRegistro.DESHABILITADO) {
            // Si después se vuelve a habilitar, los tokens emitidos antes de la baja no recuperan validez.
            usuarioRepository.invalidarSesionesDeOrganizacion(id, reloj.instant().truncatedTo(ChronoUnit.SECONDS));
        }
        trazabilidad.registrar(new Transaccion("ORGANIZACION", id, OperacionTransaccion.CAMBIO_ESTADO, id,
                anterior, actualizada, null, anterior.estado() + " -> " + nuevo));
        return actualizada;
    }

    static EstadoRegistro parsearEstado(String estado) {
        try {
            return EstadoRegistro.valueOf(estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "El estado debe ser ACTIVO o DESHABILITADO.");
        }
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El campo " + campo + " es obligatorio.");
        }
    }
}
