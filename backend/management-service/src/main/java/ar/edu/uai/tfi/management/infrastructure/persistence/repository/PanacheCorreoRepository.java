package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.EstadoCorreo;
import ar.edu.uai.tfi.management.domain.repository.CorreoRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.CorreoSalienteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@ApplicationScoped
public class PanacheCorreoRepository implements PanacheRepository<CorreoSalienteEntity>, CorreoRepository {

    /** SKIP LOCKED: dos despachos simultáneos se reparten los avisos en lugar de enviarlos dos veces. */
    static final String PENDIENTES = """
            SELECT id_correo FROM correos_salientes
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY id_correo
            LIMIT :maximo
            FOR UPDATE SKIP LOCKED""";

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public PanacheCorreoRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CorreoSaliente guardar(CorreoSaliente correo) {
        CorreoSalienteEntity entity = new CorreoSalienteEntity();
        entity.origen = correo.origen();
        entity.tipo = correo.tipo();
        entity.organizacionId = correo.organizacionId();
        entity.destinatario = correo.destinatario();
        entity.nombreDestinatario = correo.nombreDestinatario();
        entity.creadoEn = correo.creadoEn();
        copiar(correo, entity);
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<CorreoSaliente> tomarPendientes(Instant ahora, int maximo) {
        List<?> ids = getEntityManager().createNativeQuery(PENDIENTES)
                .setParameter("ahora", ahora)
                .setParameter("maximo", maximo)
                .getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Long> lista = ids.stream().map(id -> ((Number) id).longValue()).toList();
        return find("id in ?1 order by id", lista).list().stream().map(this::aDominio).toList();
    }

    @Override
    public void actualizar(CorreoSaliente correo) {
        CorreoSalienteEntity entity = findByIdOptional(correo.id())
                .orElseThrow(() -> new NoSuchElementException("No existe el correo " + correo.id() + "."));
        copiar(correo, entity);
        flush();
    }

    @Override
    public Resumen resumen(Instant desde) {
        return new Resumen(
                count("estado", EstadoCorreo.PENDIENTE),
                count("estado = ?1 and enviadoEn >= ?2", EstadoCorreo.ENVIADO, desde),
                count("estado = ?1 and creadoEn >= ?2", EstadoCorreo.FALLIDO, desde));
    }

    @Override
    public List<CorreoSaliente> ultimos(int maximo) {
        return find("order by id desc").page(0, maximo).list().stream().map(this::aDominio).toList();
    }

    private void copiar(CorreoSaliente correo, CorreoSalienteEntity entity) {
        entity.datos = aJson(correo.datos());
        entity.estado = correo.estado();
        entity.intentos = correo.intentos();
        entity.proximoIntento = correo.proximoIntento();
        entity.enviadoEn = correo.enviadoEn();
        entity.ultimoError = correo.ultimoError();
        entity.idEnvio = correo.idEnvio();
    }

    private String aJson(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos == null ? Map.of() : datos);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Los datos del correo no se pueden guardar como JSON.", exception);
        }
    }

    private CorreoSaliente aDominio(CorreoSalienteEntity entity) {
        Map<String, Object> datos;
        try {
            datos = objectMapper.readValue(entity.datos, MAPA);
        } catch (JsonProcessingException exception) {
            datos = Map.of();
        }
        return new CorreoSaliente(entity.id, entity.origen, entity.tipo, entity.organizacionId, entity.destinatario,
                entity.nombreDestinatario, datos, entity.estado, entity.intentos, entity.proximoIntento,
                entity.creadoEn, entity.enviadoEn, entity.ultimoError, entity.idEnvio);
    }
}
