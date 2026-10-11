import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Database connection details (default username is expense_app, configurable via DB_USER)
    private static final String DEFAULT_USER = "expense_app";

    public static Connection getConnection() {
        Connection connection = null;
        // Resolve host from environment variable, system property, or default to localhost
        String host = resolveHost();
        String url = "jdbc:mysql://" + host + ":3306/Expense_Tracker";

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

    private static String resolveUser() {
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

    private static String resolveHost() {
        String host = System.getenv("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            return host.trim();
        }
        host = System.getProperty("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            return host.trim();
        }
        return "localhost";
    }

    private static String resolvePassword() {
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
}
