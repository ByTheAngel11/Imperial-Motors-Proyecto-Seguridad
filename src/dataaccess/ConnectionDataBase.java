package dataaccess;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionDataBase {
    private static final Logger log = LogManager.getLogger(ConnectionDataBase.class);

    public ConnectionDataBase() {}

    public static Connection getConnection() throws SQLException {
        String url  = ConfigLoader.getDbUrl();
        String user = ConfigLoader.getDbUser();
        String pass = ConfigLoader.getDbPass();
        log.debug("Abriendo conexión a {}", sanitiseUrl(url));
        return DriverManager.getConnection(url, user, pass);
    }

    public static boolean ping(int timeoutSeconds) {
        try (Connection c = getConnection()) {
            boolean ok = c.isValid(Math.max(1, timeoutSeconds));
            if (ok) log.info("✅ Ping OK a {}", sanitiseUrl(ConfigLoader.getDbUrl()));
            else    log.warn("⚠️  Ping devolvió false para {}", sanitiseUrl(ConfigLoader.getDbUrl()));
            return ok;
        } catch (SQLException ex) {
            log.error("❌ Ping falló: {} - {}", sanitiseUrl(ConfigLoader.getDbUrl()), ex.getMessage(), ex);
            return false;
        }
    }

    public static boolean ping() { return ping(5); }

    private static String sanitiseUrl(String url) {
        if (url == null) return null;
        int at = url.indexOf('@');
        if (at > 0 && url.startsWith("jdbc:mysql://")) {
            return "jdbc:mysql://***@" + url.substring(at + 1);
        }
        return url;
    }
}
