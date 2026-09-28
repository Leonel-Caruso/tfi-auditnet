package ar.edu.uai.tfi.management.infrastructure.persistence.mongo;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraExcepciones;
import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;
import ar.edu.uai.tfi.management.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraExcepcionesRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bitácora de excepciones en MongoDB (Cosmos DB para MongoDB en Azure): colección
 * "bitacora_excepciones", compartida por ambos servicios. management-service además la consulta
 * para el panel de administración.
 */
@ApplicationScoped
public class MongoBitacoraExcepcionesRepository implements BitacoraExcepcionesRepository {

    static final String COLECCION = "bitacora_excepciones";

    private final Instance<MongoClient> mongoClient;
    private final String baseDeDatos;
    private final AtomicBoolean indiceCreado = new AtomicBoolean(false);

    public MongoBitacoraExcepcionesRepository(Instance<MongoClient> mongoClient,
                                              @ConfigProperty(name = "auditnet.mongodb.base", defaultValue = "auditnet")
                                              String baseDeDatos) {
        this.mongoClient = mongoClient;
        this.baseDeDatos = baseDeDatos;
    }

    @Override
    public void guardar(RegistroExcepcion registro) {
        coleccion().insertOne(new Document()
                .append("fecha", Date.from(registro.fecha()))
                .append("servicio", registro.servicio())
                .append("nivel", registro.nivel().name())
                .append("tipo", registro.tipo())
                .append("mensaje", registro.mensaje())
                .append("traza", registro.traza())
                .append("metodoHttp", registro.metodoHttp())
                .append("ruta", registro.ruta())
                .append("estadoHttp", registro.estadoHttp())
                .append("usuario", registro.usuario())
                .append("correlacion", registro.correlacion()));
    }

    @Override
    public List<RegistroExcepcion> buscar(FiltroBitacoraExcepciones filtro) {
        List<Bson> condiciones = new ArrayList<>();
        if (filtro.servicio() != null) {
            condiciones.add(Filters.eq("servicio", filtro.servicio()));
        }
        if (filtro.nivel() != null) {
            condiciones.add(Filters.eq("nivel", filtro.nivel().name()));
        }
        if (filtro.desde() != null) {
            condiciones.add(Filters.gte("fecha", Date.from(filtro.desde())));
        }
        if (filtro.hasta() != null) {
            condiciones.add(Filters.lte("fecha", Date.from(filtro.hasta())));
        }
        Bson consulta = condiciones.isEmpty() ? new Document() : Filters.and(condiciones);

        List<RegistroExcepcion> resultado = new ArrayList<>();
        for (Document documento : coleccion().find(consulta).sort(Sorts.descending("fecha")).limit(filtro.limite())) {
            resultado.add(aDominio(documento));
        }
        return resultado;
    }

    private MongoCollection<Document> coleccion() {
        MongoCollection<Document> coleccion = mongoClient.get().getDatabase(baseDeDatos).getCollection(COLECCION);
        if (indiceCreado.compareAndSet(false, true)) {
            coleccion.createIndex(Indexes.descending("fecha"));
        }
        return coleccion;
    }

    private RegistroExcepcion aDominio(Document documento) {
        Date fecha = documento.getDate("fecha");
        Integer estado = documento.getInteger("estadoHttp");
        return new RegistroExcepcion(
                fecha == null ? null : fecha.toInstant(),
                documento.getString("servicio"),
                NivelExcepcion.valueOf(documento.getString("nivel")),
                documento.getString("tipo"),
                documento.getString("mensaje"),
                documento.getString("traza"),
                documento.getString("metodoHttp"),
                documento.getString("ruta"),
                estado == null ? 0 : estado,
                documento.getString("usuario"),
                documento.getString("correlacion")
        );
    }
}
