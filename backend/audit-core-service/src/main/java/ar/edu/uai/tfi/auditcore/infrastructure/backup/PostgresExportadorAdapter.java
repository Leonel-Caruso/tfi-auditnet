package ar.edu.uai.tfi.auditcore.infrastructure.backup;

import ar.edu.uai.tfi.auditcore.application.port.ExportadorBaseDatosPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import javax.sql.DataSource;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Respaldo lógico de PostgreSQL con COPY (Bloque 7, D-36). Lee todas las tablas del esquema public dentro de una
 * única transacción de solo lectura con aislamiento REPEATABLE READ, así los datos son consistentes entre sí aunque
 * se estén usando los servicios. Contenido del ZIP:
 * <ul>
 *   <li>{@code datos/<tabla>.csv}: datos de cada tabla (CSV con encabezado, UTF-8);</li>
 *   <li>{@code esquema/<historial>.csv}: historial de Flyway de cada servicio (versión de la estructura);</li>
 *   <li>{@code restaurar.sql}: guion de psql para cargarlo en una base con la misma versión;</li>
 *   <li>{@code manifiesto.json}: fecha, usuario, versiones, filas y SHA-256 de cada archivo; {@code LEEME.txt}.</li>
 * </ul>
 * La bitácora de excepciones (MongoDB) no se incluye: Cosmos DB tiene sus propios respaldos automáticos.
 */
@ApplicationScoped
public class PostgresExportadorAdapter implements ExportadorBaseDatosPort {

    private static final Pattern NOMBRE = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");

    private final Instance<DataSource> dataSource;
    private final ObjectMapper objectMapper;

    public PostgresExportadorAdapter(Instance<DataSource> dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    /**
     * NOT_SUPPORTED: la conexión no se debe asociar a una transacción JTA (Agroal no permite cambiarla a solo
     * lectura ni hacer rollback de una conexión asociada). Si se llamara dentro de una transacción, se suspende.
     */
    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public ResumenExportacion exportar(OutputStream destino, MetadatosRespaldo metadatos) {
        try (Connection conexion = dataSource.get().getConnection()) {
            boolean autoCommit = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            conexion.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conexion.setReadOnly(true);
            try {
                return exportar(conexion, destino, metadatos);
            } finally {
                conexion.rollback(); // solo lectura: no hay nada que confirmar
                conexion.setReadOnly(false);
                conexion.setAutoCommit(autoCommit);
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("No se pudo generar el respaldo de la base de datos ("
                    + exception.getClass().getSimpleName() + ").", exception);
        }
    }

    ResumenExportacion exportar(Connection conexion, OutputStream destino, MetadatosRespaldo metadatos)
            throws SQLException, IOException {
        EstructuraBase estructura = leerEstructura(conexion);
        CopyManager copia = conexion.unwrap(PGConnection.class).getCopyAPI();
        List<TablaExportada> tablas = new ArrayList<>();
        Map<String, String> versiones = new LinkedHashMap<>();
        Map<String, String> hashes = new LinkedHashMap<>();

        ZipOutputStream zip = new ZipOutputStream(destino, StandardCharsets.UTF_8);
        for (String tabla : estructura.ordenRestauracion()) {
            String hash = copiarTabla(copia, zip, "datos/" + tabla + ".csv", tabla, estructura.columnas().get(tabla));
            tablas.add(new TablaExportada(tabla, contar(conexion, tabla), hash));
            hashes.put("datos/" + tabla + ".csv", hash);
        }
        for (String historial : estructura.historialesFlyway()) {
            hashes.put("esquema/" + historial + ".csv",
                    copiarTabla(copia, zip, "esquema/" + historial + ".csv", historial, estructura.columnas().get(historial)));
            versiones.put(historial, ultimaVersion(conexion, historial));
        }

        String guion = GuionRestauracion.generar(metadatos.nombre(), estructura, versiones);
        hashes.put("restaurar.sql", escribir(zip, "restaurar.sql", guion.getBytes(StandardCharsets.UTF_8)));

        Map<String, Object> manifiesto = new LinkedHashMap<>();
        manifiesto.put("formato", "auditnet-respaldo/1");
        manifiesto.put("nombre", metadatos.nombre());
        manifiesto.put("generado", metadatos.fecha().toString());
        manifiesto.put("generadoPor", metadatos.generadoPor());
        manifiesto.put("versionAplicacion", metadatos.versionAplicacion());
        manifiesto.put("versionesEsquema", versiones);
        manifiesto.put("tablas", tablas);
        manifiesto.put("archivos", hashes);
        manifiesto.put("excluido", "Bitácora de excepciones (MongoDB / Cosmos DB): tiene respaldos automáticos propios.");
        escribir(zip, "manifiesto.json",
                objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsBytes(manifiesto));
        escribir(zip, "LEEME.txt", leeme(metadatos, versiones).getBytes(StandardCharsets.UTF_8));
        zip.finish();
        return new ResumenExportacion(tablas, versiones);
    }

    EstructuraBase leerEstructura(Connection conexion) throws SQLException {
        Map<String, List<String>> columnas = new LinkedHashMap<>();
        Map<String, List<String>> secuencias = new LinkedHashMap<>();
        Map<String, Set<String>> dependencias = new LinkedHashMap<>();
        try (Statement consulta = conexion.createStatement()) {
            try (ResultSet fila = consulta.executeQuery("""
                    SELECT c.table_name, c.column_name, c.is_identity, c.column_default
                    FROM information_schema.columns c
                    JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                    WHERE c.table_schema = 'public' AND t.table_type = 'BASE TABLE' AND c.is_generated = 'NEVER'
                    ORDER BY c.table_name, c.ordinal_position""")) {
                while (fila.next()) {
                    String tabla = validar(fila.getString(1));
                    String columna = validar(fila.getString(2));
                    columnas.computeIfAbsent(tabla, clave -> new ArrayList<>()).add(columna);
                    String porDefecto = fila.getString(4);
                    if ("YES".equals(fila.getString(3)) || (porDefecto != null && porDefecto.startsWith("nextval("))) {
                        secuencias.computeIfAbsent(tabla, clave -> new ArrayList<>()).add(columna);
                    }
                }
            }
            try (ResultSet fila = consulta.executeQuery("""
                    SELECT hija.relname, padre.relname
                    FROM pg_constraint fk
                    JOIN pg_class hija ON hija.oid = fk.conrelid
                    JOIN pg_class padre ON padre.oid = fk.confrelid
                    JOIN pg_namespace esquema ON esquema.oid = hija.relnamespace
                    WHERE fk.contype = 'f' AND esquema.nspname = 'public'""")) {
                while (fila.next()) {
                    EstructuraBase.agregar(dependencias, fila.getString(1), fila.getString(2));
                }
            }
        }
        return new EstructuraBase(columnas, secuencias, dependencias);
    }

    private static String copiarTabla(CopyManager copia, ZipOutputStream zip, String entrada, String tabla,
                                      List<String> columnas) throws SQLException, IOException {
        zip.putNextEntry(new ZipEntry(entrada));
        MessageDigest digest = sha256();
        OutputStream salida = new DigestOutputStream(new SinCerrar(zip), digest);
        String lista = String.join(", ", columnas.stream().map(GuionRestauracion::id).toList());
        copia.copyOut("COPY (SELECT " + lista + " FROM " + GuionRestauracion.id(tabla) + ") TO STDOUT"
                + " WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')", salida);
        salida.flush();
        zip.closeEntry();
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String escribir(ZipOutputStream zip, String entrada, byte[] contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(entrada));
        zip.write(contenido);
        zip.closeEntry();
        return HexFormat.of().formatHex(sha256().digest(contenido));
    }

    private static long contar(Connection conexion, String tabla) throws SQLException {
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT count(*) FROM " + GuionRestauracion.id(tabla))) {
            fila.next();
            return fila.getLong(1);
        }
    }

    private static String ultimaVersion(Connection conexion, String historial) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("SELECT version FROM " + GuionRestauracion.id(historial)
                + " WHERE success ORDER BY installed_rank DESC LIMIT 1");
             ResultSet fila = consulta.executeQuery()) {
            return fila.next() ? fila.getString(1) : null;
        }
    }

    private static String validar(String nombre) {
        if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
            throw new IllegalStateException("Nombre de tabla o columna no esperado en la base: " + nombre);
        }
        return nombre;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String leeme(MetadatosRespaldo metadatos, Map<String, String> versiones) {
        StringBuilder texto = new StringBuilder()
                .append("AuditNet - respaldo ").append(metadatos.nombre()).append("\r\n")
                .append("Generado: ").append(metadatos.fecha()).append(" por ").append(metadatos.generadoPor()).append("\r\n")
                .append("Version de la aplicacion: ").append(metadatos.versionAplicacion()).append("\r\n")
                .append("Version del esquema:");
        versiones.forEach((tabla, version) -> texto.append(' ').append(tabla).append("=V").append(version));
        return texto.append("\r\n\r\n")
                .append("Contiene datos personales y hashes de contrasenas: guardarlo solo en lugares autorizados.\r\n\r\n")
                .append("Para restaurar (procedimiento completo en docs/database/RESPALDOS.md):\r\n")
                .append("1. Desplegar la misma version de AuditNet (o una base con el mismo esquema).\r\n")
                .append("2. Descomprimir este ZIP y abrir una terminal en la carpeta.\r\n")
                .append("3. psql \"host=... dbname=... user=... sslmode=require\" -f restaurar.sql\r\n")
                .append("   (con el usuario duenio de las tablas; todo ocurre en una transaccion).\r\n")
                .toString();
    }

    /** Evita que COPY cierre el ZIP al terminar cada tabla. */
    private static final class SinCerrar extends FilterOutputStream {
        SinCerrar(OutputStream destino) {
            super(destino);
        }

        @Override
        public void write(byte[] datos, int desde, int largo) throws IOException {
            out.write(datos, desde, largo);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
