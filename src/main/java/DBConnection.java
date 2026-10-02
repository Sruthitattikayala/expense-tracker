import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Database connection details
    private static final String USER = "expense_app";

    public static Connection getConnection() {
        Connection connection = null;
        // Read host from DB_HOST environment variable with fallback to localhost
        String host = System.getenv("DB_HOST");
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        String url = "jdbc:mysql://" + host + ":3306/Expense_Tracker";

        // Read password securely from the DB_PASSWORD environment variable
        String password = System.getenv("DB_PASSWORD");
        try {
            connection = DriverManager.getConnection(url, USER, password);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed: " + e.getMessage());
        }
        return connection;
    }
}
