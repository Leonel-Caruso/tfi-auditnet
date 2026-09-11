package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class OrganizacionClienteService {
    private final OrganizacionClienteRepository repository;
    private final TrazabilidadPort trazabilidad;

    public OrganizacionClienteService(OrganizacionClienteRepository repository, TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
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

        trazabilidad.registrar("ORGANIZACION_CREADA", "organizacionId=" + creada.id());
        return creada;
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El campo " + campo + " es obligatorio.");
        }
    }
}
