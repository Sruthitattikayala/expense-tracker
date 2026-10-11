import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseApiHandler implements HttpHandler {

    private final ExpenseService expenseService;
    private final Gson gson;

    public ExpenseApiHandler(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        // Handle CORS preflight
        if ("OPTIONS".equals(method)) {
            sendEmptyResponse(exchange, 204);
            return;
        }

        try {
            // Normalize path: strip trailing slash if not root context
            if (path.endsWith("/") && path.length() > "/api/expenses/".length()) {
                path = path.substring(0, path.length() - 1);
            }

            // Route 1: /api/expenses or /api/expenses/
            if (path.equals("/api/expenses") || path.equals("/api/expenses/")) {
                if ("GET".equals(method)) {
                    handleGetExpenses(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateExpense(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 2: /api/expenses/total
            if (path.equals("/api/expenses/total")) {
                if ("GET".equals(method)) {
                    handleGetTotal(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 3: /api/expenses/category-summary
            if (path.equals("/api/expenses/category-summary")) {
                if ("GET".equals(method)) {
                    handleGetCategorySummary(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 4: /api/expenses/insights
            if (path.equals("/api/expenses/insights")) {
                if ("GET".equals(method)) {
                    handleGetInsights(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route: /api/expenses/monthly-comparison
            if (path.equals("/api/expenses/monthly-comparison")) {
                if ("GET".equals(method)) {
                    handleGetMonthlyComparison(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route: /api/expenses/weekly-pace
            if (path.equals("/api/expenses/weekly-pace")) {
                if ("GET".equals(method)) {
                    handleGetWeeklyPace(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 5: /api/expenses/{id}
            if (path.startsWith("/api/expenses/")) {
                String idPart = path.substring("/api/expenses/".length());
                int id;
                try {
                    id = Integer.parseInt(idPart);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid expense ID: " + idPart);
                    return;
                }

                if ("PUT".equals(method)) {
                    handleUpdateExpense(exchange, id);
                } else if ("DELETE".equals(method)) {
                    handleDeleteExpense(exchange, id);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            sendErrorResponse(exchange, 404, "Endpoint not found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    // GET /api/expenses
    private void handleGetExpenses(HttpExchange exchange) throws IOException {
        List<Expense> expenses = expenseService.viewExpenses();
        sendJsonResponse(exchange, 200, gson.toJson(expenses));
    }

    // POST /api/expenses
    private void handleCreateExpense(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Expense exp;
        try {
            exp = gson.fromJson(body, Expense.class);
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (exp == null) {
            sendErrorResponse(exchange, 400, "Request body cannot be empty");
            return;
        }

        if (exp.getAmount() <= 0) {
            sendErrorResponse(exchange, 400, "Amount must be greater than zero");
            return;
        }

        if (exp.getCategory() == null || exp.getCategory().trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Category is required");
            return;
        }

        if (exp.getDate() == null || exp.getDate().trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Date is required (format: YYYY-MM-DD)");
            return;
        }

        if (exp.getDescription() == null) {
            exp.setDescription("");
        }

        expenseService.addExpense(exp);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Expense added successfully");
        response.put("expense", exp);
        sendJsonResponse(exchange, 201, gson.toJson(response));
    }

    // PUT /api/expenses/{id}
    private void handleUpdateExpense(HttpExchange exchange, int id) throws IOException {
        if (!expenseService.expenseExists(id)) {
            sendErrorResponse(exchange, 404, "Expense with ID " + id + " does not exist");
            return;
        }

        String body = readRequestBody(exchange);
        Expense exp;
        try {
            exp = gson.fromJson(body, Expense.class);
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (exp == null) {
            sendErrorResponse(exchange, 400, "Request body cannot be empty");
            return;
        }

        if (exp.getAmount() <= 0) {
            sendErrorResponse(exchange, 400, "Amount must be greater than zero");
            return;
        }

        if (exp.getCategory() == null || exp.getCategory().trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Category is required");
            return;
        }

        if (exp.getDate() == null || exp.getDate().trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Date is required (format: YYYY-MM-DD)");
            return;
        }

        String description = exp.getDescription() != null ? exp.getDescription().trim() : "";
        boolean updated = expenseService.updateExpense(id, exp.getAmount(), exp.getCategory().trim(), description, exp.getDate().trim());

        if (updated) {
            exp.setId(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Expense updated successfully");
            response.put("expense", exp);
            sendJsonResponse(exchange, 200, gson.toJson(response));
        } else {
            sendErrorResponse(exchange, 500, "Failed to update expense with ID " + id);
        }
    }

    // DELETE /api/expenses/{id}
    private void handleDeleteExpense(HttpExchange exchange, int id) throws IOException {
        if (!expenseService.expenseExists(id)) {
            sendErrorResponse(exchange, 404, "Expense with ID " + id + " does not exist");
            return;
        }

        boolean deleted = expenseService.deleteExpense(id);
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Expense deleted successfully");
            response.put("id", id);
            sendJsonResponse(exchange, 200, gson.toJson(response));
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete expense with ID " + id);
        }
    }

    // GET /api/expenses/total
    private void handleGetTotal(HttpExchange exchange) throws IOException {
        double total = expenseService.calculateTotalExpense();
        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        sendJsonResponse(exchange, 200, gson.toJson(response));
    }

    // GET /api/expenses/category-summary
    private void handleGetCategorySummary(HttpExchange exchange) throws IOException {
        Map<String, Integer> summary = expenseService.showCategorySummary();
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    // GET /api/expenses/insights
    private void handleGetInsights(HttpExchange exchange) throws IOException {
        Map<String, Double> insights = expenseService.showSpendingInsights();
        sendJsonResponse(exchange, 200, gson.toJson(insights));
    }

    // GET /api/expenses/monthly-comparison
    private void handleGetMonthlyComparison(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String currentMonth = getQueryParam(uri.getQuery(), "current");
        String previousMonth = getQueryParam(uri.getQuery(), "previous");
        Map<String, Object> comparison = expenseService.getMonthlyComparison(currentMonth, previousMonth);
        sendJsonResponse(exchange, 200, gson.toJson(comparison));
    }

    // GET /api/expenses/weekly-pace
    private void handleGetWeeklyPace(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String month = getQueryParam(uri.getQuery(), "month");
        expenseService.checkAndGenerateWeeklyPacing(month);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "evaluated");
        resp.put("month", month != null ? month : java.time.YearMonth.now().toString());
        sendJsonResponse(exchange, 200, gson.toJson(resp));
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length >= 2 && parts[0].equalsIgnoreCase(param)) {
                return parts[1];
            }
        }
        return null;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", errorMessage);
        errorMap.put("status", statusCode);
        sendJsonResponse(exchange, statusCode, gson.toJson(errorMap));
    }

    private void sendEmptyResponse(HttpExchange exchange, int statusCode) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
