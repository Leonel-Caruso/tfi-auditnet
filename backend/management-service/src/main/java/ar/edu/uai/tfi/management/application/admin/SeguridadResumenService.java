package ar.edu.uai.tfi.management.application.admin;

import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resumen de seguridad para el panel de administración: usuarios por estado, cuentas bloqueadas y eventos de la
 * bitácora de sistema de las últimas 24 horas.
 */
@ApplicationScoped
public class SeguridadResumenService {

    static final int LIMITE_EVENTOS = 2000;
    static final int ULTIMOS_RECHAZOS = 8;

    private final UsuarioRepository usuarioRepository;
    private final BitacoraSistemaRepository bitacoraRepository;

    public SeguridadResumenService(UsuarioRepository usuarioRepository, BitacoraSistemaRepository bitacoraRepository) {
        this.usuarioRepository = usuarioRepository;
        this.bitacoraRepository = bitacoraRepository;
    }

    public record ResumenSeguridad(long usuariosActivos, long usuariosDeshabilitados, long cuentasBloqueadas,
                                   Map<TipoEventoSistema, Long> eventosUltimas24h,
                                   List<RegistroBitacoraSistema> ultimosRechazos, Instant desde) {
    }

    public ResumenSeguridad resumen(Instant ahora) {
        List<Usuario> usuarios = usuarioRepository.listar();
        Map<Long, EstadoAcceso> accesos = usuarioRepository.estadosAcceso();
        Instant desde = ahora.minus(Duration.ofHours(24));
        List<RegistroBitacoraSistema> eventos = bitacoraRepository.buscar(
                new FiltroBitacoraSistema(null, null, null, desde, ahora, LIMITE_EVENTOS));

        Map<TipoEventoSistema, Long> porTipo = new EnumMap<>(TipoEventoSistema.class);
        for (TipoEventoSistema tipo : TipoEventoSistema.values()) {
            porTipo.put(tipo, 0L);
        }
        eventos.forEach(evento -> porTipo.merge(evento.tipo(), 1L, Long::sum));

        return new ResumenSeguridad(
                usuarios.stream().filter(u -> u.estado() == EstadoRegistro.ACTIVO).count(),
                usuarios.stream().filter(u -> u.estado() != EstadoRegistro.ACTIVO).count(),
                accesos.values().stream().filter(acceso -> acceso.bloqueado(ahora)).count(),
                porTipo,
                eventos.stream().filter(evento -> evento.resultado() == ResultadoEvento.FALLO)
                        .limit(ULTIMOS_RECHAZOS).toList(),
                desde);
    }
}
