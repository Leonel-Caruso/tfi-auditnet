package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class DispositivoService {

    private final DispositivoRepository repository;
    private final TrazabilidadPort trazabilidad;

    public DispositivoService(
            DispositivoRepository repository,
            TrazabilidadPort trazabilidad
    ) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public DispositivoRed crear(
            String nombre,
            String identificador,
            Long tipoDispositivoId,
            String fabricante,
            Long organizacionId,
            Long sedeId,
            String criticidad,
            String actor
    ) {
        validarTexto(nombre, "nombre");
        validarTexto(identificador, "identificador");
        validarTexto(fabricante, "fabricante");
        validarId(tipoDispositivoId, "tipoDispositivoId");
        validarId(organizacionId, "organizacionId");

        String identificadorNormalizado = identificador.trim().toUpperCase();
        if (repository.existePorIdentificador(identificadorNormalizado)) {
            throw new IllegalStateException(
                    "Ya existe un dispositivo con el identificador " + identificadorNormalizado + "."
            );
        }

        CriticidadDispositivo criticidadValida = parsearCriticidad(criticidad);

        DispositivoRed nuevo = new DispositivoRed(
                null,
                nombre.trim(),
                identificadorNormalizado,
                tipoDispositivoId,
                fabricante.trim(),
                organizacionId,
                sedeId,
                criticidadValida,
                EstadoDispositivo.ACTIVO
        );

        DispositivoRed creado = repository.guardar(nuevo);

        trazabilidad.registrar(
                actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor,
                "DISPOSITIVO_CREADO",
                "dispositivoId=" + creado.id() + ", identificador=" + creado.identificador()
        );

        return creado;
    }

    public List<DispositivoRed> listar() {
        return repository.listar();
    }

    public List<DispositivoRed> listarPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return repository.listarPorOrganizacion(organizacionId);
    }

    public DispositivoRed buscarPorId(Long id) {
        validarId(id, "id");
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo solicitado."));
    }

    public DispositivoRed buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return repository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo solicitado."));
    }

    private CriticidadDispositivo parsearCriticidad(String valor) {
        validarTexto(valor, "criticidad");
        try {
            return CriticidadDispositivo.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "La criticidad debe ser BAJA, MEDIA, ALTA o CRITICA."
            );
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
    }

    private void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
