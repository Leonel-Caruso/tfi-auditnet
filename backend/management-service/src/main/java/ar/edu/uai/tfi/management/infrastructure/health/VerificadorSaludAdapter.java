package ar.edu.uai.tfi.management.infrastructure.health;

import ar.edu.uai.tfi.management.application.admin.ComponenteSalud;
import ar.edu.uai.tfi.management.application.admin.SaludPort;
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
 * Verifica PostgreSQL (consulta simple y versión del esquema según Flyway) y MongoDB (comando ping). Los errores
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

    public VerificadorSaludAdapter(Instance<DataSource> dataSource,
                                   Instance<MongoClient> mongoClient,
                                   @ConfigProperty(name = "quarkus.flyway.table", defaultValue = "flyway_schema_history")
                                   String tablaFlyway,
                                   @ConfigProperty(name = "auditnet.mongodb.base", defaultValue = "auditnet")
                                   String baseMongo) {
        this.dataSource = dataSource;
        this.mongoClient = mongoClient;
        this.tablaFlyway = tablaFlyway;
        this.baseMongo = baseMongo;
    }

    @Override
    public List<ComponenteSalud> verificarComponentes() {
        return List.of(postgres(), mongo());
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

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
