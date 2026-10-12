import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the embedded HTTP REST API layer and Static Web Server.
 * Starts a real HttpServer on an ephemeral local port (port 0) and sends genuine HTTP
 * requests via java.net.http.HttpClient to verify routing, JSON handling, status codes,
 * input validation, security controls, and static file delivery.
 */
class RestApiIntegrationTest {

    private static HttpServer server;
    private static int port;
    private static String baseUrl;
    private static HttpClient httpClient;
    private static ExpenseService mockExpenseService;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void startServer() throws IOException {
        mockExpenseService = Mockito.mock(ExpenseService.class);

        // Bind to port 0 (ephemeral available port)
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/expenses", new ExpenseApiHandler(mockExpenseService));
        server.createContext("/api/budget", new BudgetApiHandler(mockExpenseService));
        server.createContext("/api/notifications", new NotificationApiHandler(mockExpenseService));
        server.createContext("/api/ai", new AiApiHandler());
        server.createContext("/", new StaticFileHandler());
        server.start();

        port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // 1. Static Web Server & Security Tests
    // =========================================================================

    @Test
    @DisplayName("Should serve index.html with 200 OK and text/html Content-Type")
    void testStaticFileServer_ServesIndexHtml() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(resp.body().contains("<!DOCTYPE html>") || resp.body().contains("<html"));
    }

    @Test
    @DisplayName("Should serve static JavaScript and CSS assets with correct MIME types")
    void testStaticFileServer_ServesJsAndCss() throws Exception {
        HttpRequest jsReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/app.js"))
                .GET()
                .build();
        HttpResponse<String> jsResp = httpClient.send(jsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, jsResp.statusCode());
        assertTrue(jsResp.headers().firstValue("Content-Type").orElse("").contains("javascript"));

        HttpRequest cssReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/style.css"))
                .GET()
                .build();
        HttpResponse<String> cssResp = httpClient.send(cssReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cssResp.statusCode());
        assertTrue(cssResp.headers().firstValue("Content-Type").orElse("").contains("css"));
    }

    @Test
    @DisplayName("Should block directory traversal attacks with 404 Not Found")
    void testStaticFileServer_BlocksDirectoryTraversal() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/../pom.xml"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
    }

    // =========================================================================
    // 2. AI Parsing REST API Endpoint (/api/ai/parse)
    // =========================================================================

    @Test
    @DisplayName("POST /api/ai/parse should successfully parse natural language text into expense intent")
    void testAiApi_ParsesNaturalLanguage() throws Exception {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("text", "Spent 1500 on groceries today");
        String jsonBody = gson.toJson(bodyMap);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/parse"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"intent\":\"EXPENSE\""));
        assertTrue(resp.body().contains("\"amount\":1500"));
        assertTrue(resp.body().contains("\"category\":\"Food\""));
    }

    @Test
    @DisplayName("POST /api/ai/parse should reject blank text with 400 Bad Request")
    void testAiApi_RejectsBlankText() throws Exception {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("text", "   ");
        String jsonBody = gson.toJson(bodyMap);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/parse"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    // =========================================================================
    // 3. Expense REST API Validation & Error Handling (/api/expenses)
    // =========================================================================

    @Test
    @DisplayName("POST /api/expenses should return 201 Created on valid input")
    void testExpenseApi_CreateExpenseSuccess() throws Exception {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("amount", 850);
        expenseMap.put("category", "Food");
        expenseMap.put("description", "Dinner with team");
        expenseMap.put("date", "2026-10-12");
        String jsonBody = gson.toJson(expenseMap);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("Expense added successfully"));
    }

    @Test
    @DisplayName("POST /api/expenses should reject negative or zero amount with 400 Bad Request")
    void testExpenseApi_RejectsNegativeAmount() throws Exception {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("amount", -50);
        expenseMap.put("category", "Food");
        expenseMap.put("description", "Invalid amount test");
        expenseMap.put("date", "2026-10-12");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(expenseMap)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("POST /api/expenses should reject missing category with 400 Bad Request")
    void testExpenseApi_RejectsMissingCategory() throws Exception {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("amount", 500);
        expenseMap.put("category", "  ");
        expenseMap.put("description", "Missing category test");
        expenseMap.put("date", "2026-10-12");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(expenseMap)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Category is required"));
    }

    @Test
    @DisplayName("POST /api/expenses should reject malformed JSON with 400 Bad Request")
    void testExpenseApi_RejectsMalformedJson() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{invalid_json_body"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} should return 404 when expense ID does not exist")
    void testExpenseApi_UpdateNonExistentReturns404() throws Exception {
        when(mockExpenseService.expenseExists(999999)).thenReturn(false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/999999"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"amount\": 100, \"category\": \"Food\", \"date\": \"2026-10-12\"}"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} with non-numeric ID should return 400 Bad Request")
    void testExpenseApi_InvalidIdFormatReturns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/invalid_id"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    @Test
    @DisplayName("OPTIONS /api/expenses should return 204 No Content for CORS preflight")
    void testExpenseApi_CorsPreflightOptions() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        assertEquals(204, resp.statusCode());
    }

    // =========================================================================
    // 4. Budget REST API Endpoints (/api/budget)
    // =========================================================================

    @Test
    @DisplayName("GET /api/budget/current should return 200 with monthly budget summary")
    void testBudgetApi_GetCurrentMonthlyBudget() throws Exception {
        MonthlyBudgetSummary summary = new MonthlyBudgetSummary("2026-10", "October 2026", 20000.0, 5000.0, 15000.0, 25.0, "Within budget");
        when(mockExpenseService.getCurrentMonthlyBudget(anyString())).thenReturn(summary);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/budget/current?month=2026-10"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"budget\":20000.0"));
        assertTrue(resp.body().contains("\"status\":\"Within budget\""));
    }

    @Test
    @DisplayName("GET /api/budget/weekly/current should return 200 with weekly budget summary")
    void testBudgetApi_GetCurrentWeeklyBudget() throws Exception {
        WeeklyBudgetSummary summary = new WeeklyBudgetSummary("2026-W41", "Week 41, 2026", "2026-10-05", "2026-10-11", "05 Oct - 11 Oct, 2026", 5000.0, 2000.0, 3000.0, 40.0, "Within budget");
        when(mockExpenseService.getCurrentWeeklyBudget(anyString())).thenReturn(summary);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/budget/weekly/current?week=2026-W41"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"week\":\"2026-W41\""));
        assertTrue(resp.body().contains("\"budget\":5000.0"));
    }

    @Test
    @DisplayName("POST /api/budget/current should reject negative budget with 400 Bad Request")
    void testBudgetApi_RejectsNegativeBudget() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("month", "2026-10");
        map.put("budget", -5000);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/budget/current"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(map)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    // =========================================================================
    // 5. Notification REST API Endpoints (/api/notifications)
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications should return 200 with notification list")
    void testNotificationApi_GetNotifications() throws Exception {
        when(mockExpenseService.getInAppNotifications()).thenReturn(Collections.emptyList());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/notifications"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test
    @DisplayName("POST /api/notifications/read-all should return 200 with success status")
    void testNotificationApi_MarkAllAsRead() throws Exception {
        when(mockExpenseService.markAllNotificationsRead()).thenReturn(true);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/notifications/read-all"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"success\":true"));
    }

    @Test
    @DisplayName("POST /api/notifications/{id}/read should return 400 when ID is non-numeric")
    void testNotificationApi_InvalidIdReturns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/notifications/abc/read"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    // =========================================================================
    // 6. Hardened Security, Method, & Input Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Static server should block URL-encoded directory traversal attempts with 404")
    void testStaticFileServer_BlocksUrlEncodedPathTraversal() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/%2e%2e/pom.xml"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
    }

    @Test
    @DisplayName("Static server HEAD /index.html should return 200 OK without body")
    void testStaticFileServer_HeadMethodReturnsHeadersOnly() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/index.html"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body() == null || resp.body().isEmpty());
    }

    @Test
    @DisplayName("Static server should reject POST requests with 405 Method Not Allowed")
    void testStaticFileServer_DisallowsPostAndPutReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/index.html"))
                .POST(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    @DisplayName("Static server should return 404 for non-existent assets")
    void testStaticFileServer_NonExistentFileReturns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/nonexistent_asset_12345.html"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
    }

    @Test
    @DisplayName("POST /api/expenses should reject zero amount with 400 Bad Request")
    void testExpenseApi_ZeroAmountRejectedWith400() throws Exception {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("amount", 0);
        expenseMap.put("category", "Food");
        expenseMap.put("description", "Zero amount");
        expenseMap.put("date", "2026-10-12");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(expenseMap)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("POST /api/expenses should reject missing date with 400 Bad Request")
    void testExpenseApi_MissingDateRejectedWith400() throws Exception {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("amount", 100);
        expenseMap.put("category", "Food");
        expenseMap.put("description", "Missing date");
        expenseMap.put("date", "   ");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(expenseMap)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Date is required"));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 200 on successful deletion")
    void testExpenseApi_DeleteSuccessReturns200() throws Exception {
        when(mockExpenseService.expenseExists(123)).thenReturn(true);
        when(mockExpenseService.deleteExpense(123)).thenReturn(true);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/123"))
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Expense deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 404 when ID does not exist")
    void testExpenseApi_DeleteNonExistentReturns404() throws Exception {
        when(mockExpenseService.expenseExists(888888)).thenReturn(false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/888888"))
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("does not exist"));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} with non-numeric ID should return 400 Bad Request")
    void testExpenseApi_DeleteInvalidIdReturns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/not_a_number"))
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Invalid expense ID"));
    }

    @Test
    @DisplayName("POST /api/expenses/total should return 405 Method Not Allowed")
    void testExpenseApi_UnsupportedMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses/total"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    @DisplayName("Responses should include standard CORS headers")
    void testExpenseApi_CorsHeadersPresentOnResponse() throws Exception {
        when(mockExpenseService.viewExpenses()).thenReturn(Collections.emptyList());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/expenses"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("*", resp.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }

    @Test
    @DisplayName("GET /api/ai/parse should return 405 Method Not Allowed")
    void testAiApi_UnsupportedMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/parse"))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    @DisplayName("DELETE /api/budget/weekly/current should return 200 on success")
    void testBudgetApi_DeleteWeeklyBudgetReturns200() throws Exception {
        when(mockExpenseService.deleteCurrentWeeklyBudget(any())).thenReturn(new WeeklyBudgetSummary());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/budget/weekly/current?week=2026-W41"))
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test
    @DisplayName("PUT /api/budget/weekly/current should return 405 Method Not Allowed")
    void testBudgetApi_UnsupportedMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/budget/weekly/current"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    @DisplayName("DELETE /api/notifications should return 405 Method Not Allowed")
    void testNotificationApi_UnsupportedMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/notifications"))
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }
}
