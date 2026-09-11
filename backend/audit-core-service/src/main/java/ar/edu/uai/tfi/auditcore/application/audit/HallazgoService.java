package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class HallazgoService {

    private final HallazgoRepository repository;

    public HallazgoService(HallazgoRepository repository) {
        this.repository = repository;
    }

    public List<HallazgoAuditoria> listarPriorizados() {
        return priorizar(repository.listar());
    }

    public List<HallazgoAuditoria> listarPriorizadosPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return priorizar(repository.listarPorOrganizacion(organizacionId));
    }

    public HallazgoAuditoria buscarPorId(Long id) {
        validarId(id, "id");
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el hallazgo solicitado."));
    }

    public HallazgoAuditoria buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return repository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe el hallazgo solicitado."));
    }

    private List<HallazgoAuditoria> priorizar(List<HallazgoAuditoria> hallazgos) {
        return hallazgos.stream()
                .sorted(Comparator
                        .comparingInt((HallazgoAuditoria h) -> rango(h.severidad())).reversed()
                        .thenComparing(HallazgoAuditoria::fechaDeteccion, Comparator.reverseOrder()))
                .toList();
    }

    private int rango(SeveridadRegla severidad) {
        return switch (severidad) {
            case BAJA -> 1;
            case MEDIA -> 2;
            case ALTA -> 3;
            case CRITICA -> 4;
        };
    }

    private void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
