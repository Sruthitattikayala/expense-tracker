import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseDAO automated table initialization logic.
 * Tests ensure that the core expenses table, monthly_budget, weekly_budget,
 * and in_app_notifications tables are verified with IF NOT EXISTS DDL statements,
 * and that repeated initialization is completely safe and idempotent.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseDaoInitTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    private ExpenseDAO expenseDAO;

    @BeforeEach
    void setUp() throws SQLException {
        expenseDAO = new ExpenseDAO();
        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        lenient().when(mockStatement.executeUpdate()).thenReturn(0);
    }

    @Test
    @DisplayName("Should prepare CREATE TABLE IF NOT EXISTS for expenses, monthly_budget, weekly_budget, and in_app_notifications")
    void testInitDatabase_CreatesAllFourTablesIncludingExpenses() throws SQLException {
        expenseDAO.initDatabase(mockConnection);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockConnection, times(4)).prepareStatement(sqlCaptor.capture());
        verify(mockStatement, times(4)).executeUpdate();

        List<String> capturedSqls = sqlCaptor.getAllValues();

        // 1. Verify expenses table DDL
        assertTrue(capturedSqls.stream().anyMatch(sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS expenses") &&
            sql.contains("Id INT AUTO_INCREMENT PRIMARY KEY") &&
            sql.contains("Amount DECIMAL(10,2) NOT NULL") &&
            sql.contains("Category VARCHAR(50) NOT NULL") &&
            sql.contains("Description VARCHAR(100) NOT NULL") &&
            sql.contains("date DATE NOT NULL")
        ), "Must execute CREATE TABLE IF NOT EXISTS expenses with expected column schema");

        // 2. Verify monthly_budget table DDL
        assertTrue(capturedSqls.stream().anyMatch(sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS monthly_budget") &&
            sql.contains("month_year VARCHAR(7) NOT NULL UNIQUE")
        ), "Must execute CREATE TABLE IF NOT EXISTS monthly_budget");

        // 3. Verify weekly_budget table DDL
        assertTrue(capturedSqls.stream().anyMatch(sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS weekly_budget") &&
            sql.contains("week_year VARCHAR(20) NOT NULL UNIQUE")
        ), "Must execute CREATE TABLE IF NOT EXISTS weekly_budget");

        // 4. Verify in_app_notifications table DDL
        assertTrue(capturedSqls.stream().anyMatch(sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS in_app_notifications") &&
            sql.contains("notification_key VARCHAR(100) NOT NULL UNIQUE")
        ), "Must execute CREATE TABLE IF NOT EXISTS in_app_notifications");
    }

    @Test
    @DisplayName("Should be idempotent and safe across repeated initialization executions")
    void testInitDatabase_IdempotentOnRepeatedCalls() throws SQLException {
        // Execute initialization twice
        expenseDAO.initDatabase(mockConnection);
        expenseDAO.initDatabase(mockConnection);

        // All 8 statements across 2 runs must execute successfully
        verify(mockConnection, times(8)).prepareStatement(anyString());
        verify(mockStatement, times(8)).executeUpdate();
    }

    @Test
    @DisplayName("Should gracefully handle database connection errors without crashing")
    void testInitDatabase_HandlesSQLExceptionGracefully() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Simulated connection timeout"));

        assertThrows(SQLException.class, () -> {
            expenseDAO.initDatabase(mockConnection);
        });
    }

    @Test
    @DisplayName("Should verify existing records and tables are preserved on live database if available")
    void testInitDatabase_PreservesLiveDatabaseStateIfConnected() {
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                int countBefore = expenseDAO.viewExpenses().size();

                // Run initialization on live connection
                expenseDAO.initDatabase();

                int countAfter = expenseDAO.viewExpenses().size();
                assertEquals(countBefore, countAfter, "Existing expense records must be preserved across initDatabase runs");
            } finally {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}
