import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for database connectivity, schema integrity,
 * and isolated data operations against the live MySQL Expense_Tracker instance.
 *
 * Guarantees zero pollution or data loss for existing user records by:
 * 1. Using uniquely tagged UUID test records.
 * 2. Enforcing cleanup in try-finally blocks.
 * 3. Verifying initial and post-test row counts and exact pre-existing ID sets are strictly identical.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    private static ExpenseDAO expenseDAO;
    private static ExpenseService expenseService;
    private static boolean dbAvailable;

    @BeforeAll
    static void checkDatabaseAvailability() {
        expenseDAO = new ExpenseDAO();
        expenseService = new ExpenseService();

        try (Connection conn = DBConnection.getConnection()) {
            dbAvailable = (conn != null && !conn.isClosed());
            if (dbAvailable) {
                // Ensure all core tables are verified and created before any test queries execute
                expenseDAO.initDatabase(conn);
            }
        } catch (SQLException e) {
            dbAvailable = false;
        }
    }

    @BeforeEach
    void ensureDatabaseInitialized() {
        if (dbAvailable) {
            try (Connection conn = DBConnection.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    expenseDAO.initDatabase(conn);
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private void assumeDatabaseOnline() {
        Assumptions.assumeTrue(dbAvailable, "Live MySQL database is not accessible; skipping live database test.");
    }

    @Test
    @Order(1)
    @DisplayName("Verify live MySQL connectivity and required schema tables exist")
    void testDatabaseConnectivityAndSchema() throws SQLException {
        assumeDatabaseOnline();

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Database connection should not be null");
            assertTrue(conn.isValid(2), "Database connection should be valid within 2 seconds");

            // Verify auto-initialization executes without errors
            expenseDAO.initDatabase(conn);

            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "expenses", null)) {
                assertTrue(rs.next(), "Table 'expenses' must exist in the database");
            }
            try (ResultSet rs = metaData.getTables(null, null, "monthly_budget", null)) {
                assertTrue(rs.next(), "Table 'monthly_budget' must exist in the database");
            }
            try (ResultSet rs = metaData.getTables(null, null, "weekly_budget", null)) {
                assertTrue(rs.next(), "Table 'weekly_budget' must exist in the database");
            }
            try (ResultSet rs = metaData.getTables(null, null, "in_app_notifications", null)) {
                assertTrue(rs.next(), "Table 'in_app_notifications' must exist in the database");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify isolated expense CRUD lifecycle with guaranteed cleanup and zero data loss")
    void testIsolatedExpenseCrudLifecycle() {
        assumeDatabaseOnline();

        List<Expense> initialExpenses = expenseDAO.viewExpenses();
        int initialCount = initialExpenses.size();
        Set<Integer> initialIds = initialExpenses.stream().map(Expense::getId).collect(Collectors.toSet());

        String uuidShort = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTag = "E2E_" + uuidShort;
        Expense testExpense = new Expense();
        testExpense.setAmount(375);
        testExpense.setCategory("Shopping");
        testExpense.setDescription(uniqueTag);
        testExpense.setDate("2026-10-12");

        expenseDAO.addExpense(testExpense);
        int generatedId = testExpense.getId();
        assertTrue(generatedId > 0, "Generated ID should be positive");

        try {
            // 1. Verify existence
            assertTrue(expenseDAO.expenseExists(generatedId), "Created expense should exist");

            // 2. Verify retrieval and data integrity
            Expense retrieved = expenseDAO.getExpenseById(generatedId);
            assertNotNull(retrieved, "Retrieved expense should not be null");
            assertEquals(375, retrieved.getAmount());
            assertEquals("Shopping", retrieved.getCategory());
            assertEquals(uniqueTag, retrieved.getDescription());
            assertEquals("2026-10-12", retrieved.getDate());

            // 3. Verify update
            String updatedTag = "UPD_" + uuidShort;
            boolean updated = expenseDAO.updateExpense(generatedId, 450, "Shopping", updatedTag, "2026-10-12");
            assertTrue(updated, "Update operation should return true");

            Expense updatedExpense = expenseDAO.getExpenseById(generatedId);
            assertNotNull(updatedExpense);
            assertEquals(450, updatedExpense.getAmount());
            assertEquals(updatedTag, updatedExpense.getDescription());

        } finally {
            // 4. Guaranteed cleanup: Delete test record
            boolean deleted = expenseDAO.deleteExpense(generatedId);
            assertTrue(deleted, "CRITICAL: Cleanup failed to delete test expense ID: " + generatedId);

            // 5. Verify post-cleanup state and row count preservation
            assertFalse(expenseDAO.expenseExists(generatedId), "Deleted test expense should no longer exist");
            assertNull(expenseDAO.getExpenseById(generatedId), "Querying deleted ID should return null");

            List<Expense> finalExpenses = expenseDAO.viewExpenses();
            Set<Integer> finalIds = finalExpenses.stream().map(Expense::getId).collect(Collectors.toSet());
            assertEquals(initialCount, finalExpenses.size(),
                    "Total expense count after test cleanup must match initial count (zero user data lost)");
            assertEquals(initialIds, finalIds,
                    "Pre-existing user expense IDs must remain completely unchanged");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Verify full-stack HTTP REST API with live database: create, retrieve, update, delete")
    void testFullStackRestApiWithLiveDatabase() throws Exception {
        assumeDatabaseOnline();

        // Ensure database tables are initialized matching Requirements.java startup
        expenseService.initDatabase();

        // Start a real embedded server wired with the real ExpenseService and live MySQL
        HttpServer liveServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        liveServer.createContext("/api/expenses", new ExpenseApiHandler(expenseService));
        liveServer.start();

        int livePort = liveServer.getAddress().getPort();
        String liveUrl = "http://127.0.0.1:" + livePort;
        HttpClient client = HttpClient.newHttpClient();

        List<Expense> initialExpenses = expenseDAO.viewExpenses();
        int initialCount = initialExpenses.size();
        Set<Integer> initialIds = initialExpenses.stream().map(Expense::getId).collect(Collectors.toSet());

        String uuidShort = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTag = "FS_" + uuidShort;
        int createdId = -1;

        try {
            // 1. POST /api/expenses via HTTP
            String jsonPayload = String.format("{\"amount\": 275, \"category\": \"Shopping\", \"description\": \"%s\", \"date\": \"2026-10-12\"}", uniqueTag);
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(liveUrl + "/api/expenses"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(201, postResp.statusCode());
            assertTrue(postResp.body().contains("Expense added successfully"));

            JsonObject respJson = JsonParser.parseString(postResp.body()).getAsJsonObject();
            createdId = respJson.getAsJsonObject("expense").get("id").getAsInt();
            assertTrue(createdId > 0, "Created expense ID must be positive");

            // 2. GET /api/expenses via HTTP - verify it appears in live collection
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create(liveUrl + "/api/expenses"))
                    .GET()
                    .build();
            HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, getResp.statusCode());
            assertTrue(getResp.body().contains(uniqueTag));

            // 3. PUT /api/expenses/{id} via HTTP - verify live update
            String updatedTag = "FSU_" + uuidShort;
            String updatePayload = String.format("{\"amount\": 350, \"category\": \"Shopping\", \"description\": \"%s\", \"date\": \"2026-10-12\"}", updatedTag);
            HttpRequest putReq = HttpRequest.newBuilder()
                    .uri(URI.create(liveUrl + "/api/expenses/" + createdId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(updatePayload))
                    .build();
            HttpResponse<String> putResp = client.send(putReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, putResp.statusCode());
            assertTrue(putResp.body().contains("Expense updated successfully"));

            // Verify update reached the database directly
            Expense fromDb = expenseDAO.getExpenseById(createdId);
            assertNotNull(fromDb);
            assertEquals(350, fromDb.getAmount());
            assertEquals(updatedTag, fromDb.getDescription());

        } finally {
            try {
                // 4. DELETE /api/expenses/{id} via HTTP
                if (createdId > 0 && expenseDAO.expenseExists(createdId)) {
                    HttpRequest delReq = HttpRequest.newBuilder()
                            .uri(URI.create(liveUrl + "/api/expenses/" + createdId))
                            .DELETE()
                            .build();
                    HttpResponse<String> delResp = client.send(delReq, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, delResp.statusCode());
                    assertTrue(delResp.body().contains("Expense deleted successfully"));
                }
            } finally {
                liveServer.stop(0);

                // Verify pre-existing user records are 100% preserved
                List<Expense> finalExpenses = expenseDAO.viewExpenses();
                Set<Integer> finalIds = finalExpenses.stream().map(Expense::getId).collect(Collectors.toSet());
                assertEquals(initialCount, finalExpenses.size(),
                        "Total count must be strictly restored to initial count");
                assertEquals(initialIds, finalIds,
                        "Pre-existing user expense IDs must remain completely unchanged");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Verify live calculation queries execute safely without SQL errors")
    void testLiveCalculationsAndBudgetQueries() {
        assumeDatabaseOnline();

        // Total expense calculation
        double total = expenseDAO.calculateTotalExpense();
        assertTrue(total >= 0.0, "Total expense should be non-negative");

        // Category summary
        Map<String, Integer> categorySummary = expenseDAO.showCategorySummary();
        assertNotNull(categorySummary, "Category summary map should not be null");

        // Spending insights
        Map<String, Double> insights = expenseDAO.showSpendingInsights();
        assertNotNull(insights, "Spending insights map should not be null");

        // Monthly budget retrieval
        String curMonth = YearMonth.now().toString();
        BudgetPlan monthlyBudget = expenseDAO.getBudget(curMonth);

        // In-app notifications
        List<NotificationItem> notifications = expenseDAO.getNotifications();
        assertNotNull(notifications, "Notifications list should not be null");
    }

    @Test
    @Order(5)
    @DisplayName("Verify ExpenseService integrated operations against live database")
    void testExpenseServiceLiveIntegration() {
        assumeDatabaseOnline();

        // Month-over-month comparison
        String currentMonth = YearMonth.now().toString();
        YearMonth curYM = YearMonth.parse(currentMonth);
        String prevMonth = curYM.minusMonths(1).toString();

        Map<String, Object> comparison = expenseService.getMonthlyComparison(currentMonth, prevMonth);
        assertNotNull(comparison, "Month-over-month comparison should not be null");
        assertTrue(comparison.containsKey("currentMonth"));
        assertTrue(comparison.containsKey("previousMonth"));
        assertTrue(comparison.containsKey("currentTotal"));
        assertTrue(comparison.containsKey("previousTotal"));
        assertTrue(comparison.containsKey("percentageChange"));

        // Current monthly budget summary
        MonthlyBudgetSummary monthlySummary = expenseService.getCurrentMonthlyBudget(currentMonth);
        assertNotNull(monthlySummary, "Current monthly budget summary should not be null");
        assertEquals(currentMonth, monthlySummary.getMonth());
        assertNotNull(monthlySummary.getStatus());

        // Current weekly budget summary
        WeeklyBudgetSummary weeklySummary = expenseService.getCurrentWeeklyBudget();
        assertNotNull(weeklySummary, "Current weekly budget summary should not be null");
        assertNotNull(weeklySummary.getWeek());
        assertNotNull(weeklySummary.getStatus());

        // Notifications retrieval
        List<NotificationItem> notifs = expenseService.getInAppNotifications();
        assertNotNull(notifs, "In-app notifications list should not be null");
    }
}
