package ar.edu.uai.tfi.auditcore.infrastructure.health;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import ar.edu.uai.tfi.auditcore.application.admin.ComponenteSalud;
import ar.edu.uai.tfi.auditcore.application.admin.SaludPort;
import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Verifica PostgreSQL (consulta simple y versión del esquema de audit-core según Flyway) y MongoDB (ping). Los errores
 * se informan con su tipo, nunca con cadenas de conexión ni credenciales.
 */
@ApplicationScoped
public class VerificadorSaludAdapter implements SaludPort {

    private static final Logger LOG = Logger.getLogger(VerificadorSaludAdapter.class);
    private static final Pattern NOMBRE_TABLA = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");

    private final Instance<DataSource> dataSource;
    private final Instance<MongoClient> mongoClient;
    private final String tablaFlyway;
    private final String baseMongo;
    private final AlmacenArchivosPort almacen;

    public VerificadorSaludAdapter(Instance<DataSource> dataSource,
                                   Instance<MongoClient> mongoClient,
                                   @ConfigProperty(name = "quarkus.flyway.table", defaultValue = "flyway_schema_history")
                                   String tablaFlyway,
                                   @ConfigProperty(name = "auditnet.mongodb.base", defaultValue = "auditnet")
                                   String baseMongo,
                                   AlmacenArchivosPort almacen) {
        this.dataSource = dataSource;
        this.mongoClient = mongoClient;
        this.tablaFlyway = tablaFlyway;
        this.baseMongo = baseMongo;
        this.almacen = almacen;
    }

    @Override
    public List<ComponenteSalud> verificarComponentes() {
        return List.of(postgres(), mongo(), repositorioArchivos());
    }

    private ComponenteSalud postgres() {
        long inicio = System.nanoTime();
        if (!NOMBRE_TABLA.matcher(tablaFlyway).matches()) {
            return new ComponenteSalud("PostgreSQL", ComponenteSalud.DOWN, 0, "Nombre de tabla de Flyway inválido.");
        }
        try (Connection conexion = dataSource.get().getConnection();
             PreparedStatement consulta = conexion.prepareStatement(
                     "SELECT version FROM " + tablaFlyway + " WHERE success ORDER BY installed_rank DESC LIMIT 1");
             ResultSet fila = consulta.executeQuery()) {
            String version = fila.next() ? fila.getString(1) : "sin migraciones";
            return new ComponenteSalud("PostgreSQL", ComponenteSalud.UP, milisDesde(inicio), "esquema V" + version);
        } catch (Exception exception) {
            LOG.warnf("Salud: PostgreSQL no responde (%s)", exception.getClass().getSimpleName());
            return new ComponenteSalud("PostgreSQL", ComponenteSalud.DOWN, milisDesde(inicio),
                    "No responde (" + exception.getClass().getSimpleName() + ")");
        }
    }

    private ComponenteSalud mongo() {
        long inicio = System.nanoTime();
        try {
            mongoClient.get().getDatabase(baseMongo).runCommand(new Document("ping", 1));
            return new ComponenteSalud("MongoDB", ComponenteSalud.UP, milisDesde(inicio),
                    "bitácora de excepciones · base " + baseMongo);
        } catch (Exception exception) {
            LOG.warnf("Salud: MongoDB no responde (%s)", exception.getClass().getSimpleName());
            return new ComponenteSalud("MongoDB", ComponenteSalud.DOWN, milisDesde(inicio),
                    "No responde (" + exception.getClass().getSimpleName() + ")");
        }
    }

    /** Blob Storage en Azure, Azurite en local. */
    private ComponenteSalud repositorioArchivos() {
        String nombre = "Repositorio de archivos";
        if (!almacen.configurado()) {
            return new ComponenteSalud(nombre, ComponenteSalud.DOWN, 0,
                    "No configurado: las configuraciones y reportes quedan pendientes de subir");
        }
        long inicio = System.nanoTime();
        try {
            almacen.verificar();
            return new ComponenteSalud(nombre, ComponenteSalud.UP, milisDesde(inicio),
                    "Blob Storage · contenedor privado " + almacen.contenedor());
        } catch (AlmacenArchivosPort.AlmacenamientoException exception) {
            if (exception.estadoHttp() == 404) {
                return new ComponenteSalud(nombre, ComponenteSalud.UP, milisDesde(inicio),
                        "Blob Storage · el contenedor " + almacen.contenedor() + " se crea con el primer archivo");
            }
            LOG.warnf("Salud: repositorio de archivos no responde (%s)", exception.getMessage());
            return new ComponenteSalud(nombre, ComponenteSalud.DOWN, milisDesde(inicio), exception.getMessage());
        } catch (Exception exception) {
            LOG.warnf("Salud: repositorio de archivos no responde (%s)", exception.getClass().getSimpleName());
            return new ComponenteSalud(nombre, ComponenteSalud.DOWN, milisDesde(inicio),
                    "No responde (" + exception.getClass().getSimpleName() + ")");
        }
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
