package dataaccess;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public final class ConfigLoader {
    private static final Logger log = LogManager.getLogger(ConfigLoader.class);

    private static final String BASE_FILE = "/db.properties";
    private static final String ENV_KEY   = "app.env";  // si existe, buscar db-<env>.properties
    private static final Properties PROPS = new Properties();

    static {
        load();
    }

    private ConfigLoader() {}

    public static void load() {
        PROPS.clear();

        loadFromClasspath(BASE_FILE, true);

        String env = PROPS.getProperty(ENV_KEY, System.getProperty(ENV_KEY, System.getenv("APP_ENV")));
        if (env != null && !env.isBlank()) {
            String envFile = "/db-" + env.trim() + ".properties";
            loadFromClasspath(envFile, false);
        }

        overrideFromEnv("DB_URL",  "db.url");
        overrideFromEnv("DB_USER", "db.user");
        overrideFromEnv("DB_PASS", "db.pass");

        overrideFromSys("DB_URL",  "db.url");
        overrideFromSys("DB_USER", "db.user");
        overrideFromSys("DB_PASS", "db.pass");

        log.info("Config cargada. URL={}, user={}, env={}",
                sanitiseUrl(PROPS.getProperty("db.url")),
                safe(PROPS.getProperty("db.user")),
                safe(env));
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String def) {
        return PROPS.getProperty(key, def);
    }

    public static String getDbUrl()  { return mustGet("db.url"); }
    public static String getDbUser() { return mustGet("db.user"); }
    public static String getDbPass() { return mustGet("db.pass"); }


    private static void loadFromClasspath(String path, boolean required) {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("No se encontró " + path + " en el classpath.");
                } else {
                    log.warn("No se encontró {} (opcional). Se continúa con configuración base.", path);
                    return;
                }
            }
            Properties p = new Properties();
            p.load(in);
            PROPS.putAll(p);
            if (!Objects.equals(path, BASE_FILE)) {
                log.info("Superpuesta configuración de {}", path);
            }
        } catch (Exception ex) {
            String msg = "Error cargando " + path + ": " + ex.getMessage();
            if (required) throw new IllegalStateException(msg, ex);
            log.warn(msg, ex);
        }
    }

    private static void overrideFromEnv(String envKey, String propKey) {
        String val = System.getenv(envKey);
        if (val != null && !val.isBlank()) {
            PROPS.setProperty(propKey, val);
            log.info("Override por ENV {} -> {}", envKey, propKey);
        }
    }

    private static void overrideFromSys(String sysKey, String propKey) {
        String val = System.getProperty(sysKey);
        if (val != null && !val.isBlank()) {
            PROPS.setProperty(propKey, val);
            log.info("Override por System Property {} -> {}", sysKey, propKey);
        }
    }

    private static String mustGet(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta propiedad requerida: " + key);
        }
        return v;
    }

    private static String sanitiseUrl(String url) {
        if (url == null) return null;
        int at = url.indexOf('@');
        if (at > 0 && url.startsWith("jdbc:mysql://")) {
            return "jdbc:mysql://***@" + url.substring(at + 1);
        }
        return url;
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "<nulo>" : s;
    }

    public static Properties snapshot() {
        Properties copy = new Properties();
        for (Map.Entry<Object, Object> e : PROPS.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }
}
