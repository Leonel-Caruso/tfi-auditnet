package ar.edu.uai.tfi.management.application.admin;

import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Resumen de seguridad del panel de administración. */
class SeguridadResumenServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-11T12:00:00Z");

    @Test
    void cuentaUsuariosBloqueosYEventosDeLasUltimas24Horas() {
        EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
        usuarios.agregar(usuario(1L, EstadoRegistro.ACTIVO));
        usuarios.agregar(usuario(2L, EstadoRegistro.ACTIVO));
        usuarios.agregar(usuario(3L, EstadoRegistro.DESHABILITADO));
        usuarios.accesos.put(2L, new EstadoAcceso(2L, 0, AHORA.plusSeconds(300), null, null));
        usuarios.accesos.put(1L, new EstadoAcceso(1L, 0, AHORA.minusSeconds(300), null, null)); // ya vencido

        List<FiltroBitacoraSistema> filtros = new ArrayList<>();
        BitacoraSistemaRepository bitacora = new BitacoraSistemaRepository() {
            @Override
            public RegistroBitacoraSistema guardar(RegistroBitacoraSistema registro) {
                return registro;
            }

            @Override
            public List<RegistroBitacoraSistema> buscar(FiltroBitacoraSistema filtro) {
                filtros.add(filtro);
                List<RegistroBitacoraSistema> registros = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    registros.add(registro(TipoEventoSistema.LOGIN_RECHAZADO, ResultadoEvento.FALLO));
                }
                registros.add(registro(TipoEventoSistema.LOGIN_EXITOSO, ResultadoEvento.EXITO));
                registros.add(registro(TipoEventoSistema.CUENTA_BLOQUEADA, ResultadoEvento.FALLO));
                return registros;
            }
        };

        SeguridadResumenService.ResumenSeguridad resumen = new SeguridadResumenService(usuarios, bitacora).resumen(AHORA);

        assertEquals(2, resumen.usuariosActivos());
        assertEquals(1, resumen.usuariosDeshabilitados());
        assertEquals(1, resumen.cuentasBloqueadas());
        assertEquals(10L, resumen.eventosUltimas24h().get(TipoEventoSistema.LOGIN_RECHAZADO));
        assertEquals(1L, resumen.eventosUltimas24h().get(TipoEventoSistema.CUENTA_BLOQUEADA));
        assertEquals(0L, resumen.eventosUltimas24h().get(TipoEventoSistema.USUARIO_CREADO));
        assertEquals(SeguridadResumenService.ULTIMOS_RECHAZOS, resumen.ultimosRechazos().size());
        assertEquals(AHORA.minus(Duration.ofHours(24)), filtros.get(0).desde());
        assertEquals(AHORA, filtros.get(0).hasta());
    }

    private static Usuario usuario(Long id, EstadoRegistro estado) {
        return new Usuario(id, 1L, "U" + id, "u" + id + "@x.com", "u" + id, "hash", estado, Set.of("ANALISTA_RED"));
    }

    private static RegistroBitacoraSistema registro(TipoEventoSistema tipo, ResultadoEvento resultado) {
        return new RegistroBitacoraSistema(1L, AHORA, tipo, resultado, "ana", 1L, 1L, "127.0.0.1", "test", "c", "");
    }
}
