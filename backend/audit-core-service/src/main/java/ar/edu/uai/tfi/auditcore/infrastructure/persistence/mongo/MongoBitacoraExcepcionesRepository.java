package ar.edu.uai.tfi.auditcore.infrastructure.persistence.mongo;

import ar.edu.uai.tfi.auditcore.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.auditcore.domain.repository.BitacoraExcepcionesRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bitácora de excepciones en MongoDB (Cosmos DB para MongoDB en Azure): colección
 * "bitacora_excepciones", compartida por ambos servicios.
 * Cada error se guarda como un documento; el índice por fecha permite ordenar y filtrar.
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
        MongoCollection<Document> coleccion = mongoClient.get().getDatabase(baseDeDatos).getCollection(COLECCION);
        if (indiceCreado.compareAndSet(false, true)) {
            coleccion.createIndex(Indexes.descending("fecha"));
        }
        coleccion.insertOne(new Document()
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
}
