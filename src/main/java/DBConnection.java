import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Database connection details (default username is expense_app, configurable via DB_USER)
    public static final String DEFAULT_USER = "expense_app";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;
    public static final String DEFAULT_DATABASE = "Expense_Tracker";

    public static Connection getConnection() {
        Connection connection = null;
        // Resolve host and port from environment variable, system property, or default
        String host = resolveHost();
        int port = resolvePort();
        String url = buildJdbcUrl(host, port);

        // Read user and password securely from environment variables, system properties, or fallbacks
        String user = resolveUser();
        String password = resolvePassword();
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed: " + e.getMessage());
        }
        return connection;
    }

    public static int resolvePort() {
        String port = System.getenv("DB_PORT");
        if (port != null && !port.trim().isEmpty()) {
            try {
                int p = Integer.parseInt(port.trim());
                if (p > 0 && p <= 65535) {
                    return p;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        port = System.getProperty("DB_PORT");
        if (port != null && !port.trim().isEmpty()) {
            try {
                int p = Integer.parseInt(port.trim());
                if (p > 0 && p <= 65535) {
                    return p;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_PORT;
    }

    public static String resolveUser() {
        String user = System.getenv("DB_USER");
        if (user != null && !user.trim().isEmpty()) {
            return user.trim();
        }
        user = System.getProperty("DB_USER");
        if (user != null && !user.trim().isEmpty()) {
            return user.trim();
        }
        return DEFAULT_USER;
    }

    public static String resolveHost() {
        String host = System.getenv("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            return host.trim();
        }
        host = System.getProperty("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            return host.trim();
        }
        return DEFAULT_HOST;
    }

    public static String resolvePassword() {
        // 1. Environment variable (standard for container / cloud / terminal)
        String pwd = System.getenv("DB_PASSWORD");
        if (pwd != null && !pwd.trim().isEmpty()) {
            return pwd.trim();
        }

        // 2. JVM System property (e.g. -DDB_PASSWORD=...)
        pwd = System.getProperty("DB_PASSWORD");
        if (pwd != null && !pwd.trim().isEmpty()) {
            return pwd.trim();
        }

        // 3. Fallback to local secret configuration file if present
        try {
            File secretFile = new File("k8s/secret.yaml");
            if (secretFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(secretFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("DB_PASSWORD:")) {
                            String val = line.substring("DB_PASSWORD:".length()).trim();
                            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isEmpty()) {
                                return val;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public static boolean isLocalHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return true;
        }
        String h = host.trim().toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1")
                || h.equals("host.docker.internal");
    }

    public static String resolveSslMode(String host) {
        String sslMode = System.getenv("DB_SSL_MODE");
        if (sslMode != null && !sslMode.trim().isEmpty()) {
            return sslMode.trim();
        }
        sslMode = System.getProperty("DB_SSL_MODE");
        if (sslMode != null && !sslMode.trim().isEmpty()) {
            return sslMode.trim();
        }

        // If host is remote (such as TiDB Cloud), enforce secure TLS with certificate verification
        if (!isLocalHost(host)) {
            return "VERIFY_IDENTITY";
        }

        // Preserve compatibility with local MySQL by default
        return null;
    }

    public static String buildJdbcUrl(String host, int port) {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(DEFAULT_DATABASE);

        String sslMode = resolveSslMode(host);
        if (sslMode != null && !sslMode.isEmpty()) {
            url.append("?sslMode=").append(sslMode);
            if ("VERIFY_IDENTITY".equalsIgnoreCase(sslMode) || "VERIFY_CA".equalsIgnoreCase(sslMode) || "REQUIRED".equalsIgnoreCase(sslMode)) {
                url.append("&enabledTLSProtocols=TLSv1.2,TLSv1.3");
            }
        }
        return url.toString();
    }

    public static String buildJdbcUrl() {
        return buildJdbcUrl(resolveHost(), resolvePort());
    }
}
