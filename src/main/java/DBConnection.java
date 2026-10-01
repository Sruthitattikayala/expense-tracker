import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Database connection details
    private static final String URL = "jdbc:mysql://localhost:3306/Expense_Tracker";
    private static final String USER = "root";

    public static Connection getConnection() {
        Connection connection = null;
        // Read password securely from the DB_PASSWORD environment variable
        String password = System.getenv("DB_PASSWORD");
        try {
            connection = DriverManager.getConnection(URL, USER, password);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed: " + e.getMessage());
        }
        return connection;
    }
}
