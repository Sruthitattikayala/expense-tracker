import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetApiHandler implements HttpHandler {

    private final ExpenseService expenseService;
    private final Gson gson;

    public BudgetApiHandler(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.gson = new Gson();
    }

    public static class BudgetRequest {
        public int salary;
        public List<String> categories;
        public String monthYear;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if ("OPTIONS".equals(method)) {
            sendEmptyResponse(exchange, 204);
            return;
        }

        try {
            // Route 0: /api/budget/weekly/current or /api/budget/weekly (Weekly budget for the current week)
            if (path.equals("/api/budget/weekly/current") || path.equals("/api/budget/weekly/current/")
                    || path.equals("/api/budget/weekly") || path.equals("/api/budget/weekly/")) {
                if ("GET".equals(method)) {
                    handleGetCurrentWeeklyBudget(exchange, uri);
                } else if ("POST".equals(method)) {
                    handleSaveCurrentWeeklyBudget(exchange);
                } else if ("DELETE".equals(method)) {
                    handleDeleteCurrentWeeklyBudget(exchange, uri);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 1: /api/budget/current (Monthly budget for the current month)
            if (path.equals("/api/budget/current") || path.equals("/api/budget/current/")) {
                if ("GET".equals(method)) {
                    handleGetCurrentBudget(exchange, uri);
                } else if ("POST".equals(method)) {
                    handleSaveCurrentBudget(exchange);
                } else if ("DELETE".equals(method)) {
                    handleDeleteCurrentBudget(exchange, uri);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 2: /api/budget/guidelines (Suggested proportion guidelines)
            if (path.startsWith("/api/budget/guidelines")) {
                if ("GET".equals(method)) {
                    handleGetBudget(exchange, uri);
                } else if ("POST".equals(method)) {
                    handleSaveBudget(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // Route 3: /api/budget or /api/budget/
            if (path.equals("/api/budget") || path.equals("/api/budget/")) {
                String type = getQueryParam(uri.getQuery(), "type");
                if ("current".equalsIgnoreCase(type)) {
                    if ("GET".equals(method)) {
                        handleGetCurrentBudget(exchange, uri);
                    } else if ("POST".equals(method)) {
                        handleSaveCurrentBudget(exchange);
                    } else {
                        sendErrorResponse(exchange, 405, "Method Not Allowed");
                    }
                } else {
                    if ("GET".equals(method)) {
                        handleGetBudget(exchange, uri);
                    } else if ("POST".equals(method)) {
                        handleSaveBudget(exchange);
                    } else {
                        sendErrorResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                return;
            }

            sendErrorResponse(exchange, 404, "Endpoint Not Found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleGetCurrentWeeklyBudget(HttpExchange exchange, URI uri) throws IOException {
        String week = getQueryParam(uri.getQuery(), "week");
        WeeklyBudgetSummary summary = expenseService.getCurrentWeeklyBudget(week);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleSaveCurrentWeeklyBudget(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        double budgetAmount = 0.0;
        String week = null;

        try {
            Map<String, Object> req = gson.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType());
            if (req != null) {
                if (req.containsKey("budget")) {
                    budgetAmount = ((Number) req.get("budget")).doubleValue();
                } else if (req.containsKey("amount")) {
                    budgetAmount = ((Number) req.get("amount")).doubleValue();
                }
                if (req.containsKey("week")) {
                    week = (String) req.get("week");
                } else if (req.containsKey("weekYear")) {
                    week = (String) req.get("weekYear");
                }
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (budgetAmount <= 0) {
            sendErrorResponse(exchange, 400, "Weekly budget amount must be greater than 0");
            return;
        }

        WeeklyBudgetSummary summary = expenseService.saveCurrentWeeklyBudget(week, budgetAmount);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleDeleteCurrentWeeklyBudget(HttpExchange exchange, URI uri) throws IOException {
        String week = getQueryParam(uri.getQuery(), "week");
        WeeklyBudgetSummary summary = expenseService.deleteCurrentWeeklyBudget(week);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleGetCurrentBudget(HttpExchange exchange, URI uri) throws IOException {
        String month = getQueryParam(uri.getQuery(), "month");
        if (month == null || month.trim().isEmpty()) {
            month = YearMonth.now().toString();
        }

        expenseService.checkAndGenerateMonthlyBudgetNotifications(month);
        MonthlyBudgetSummary summary = expenseService.getCurrentMonthlyBudget(month);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleSaveCurrentBudget(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        int budgetAmount = 0;
        String month = null;

        try {
            Map<String, Object> req = gson.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType());
            if (req != null) {
                if (req.containsKey("budget")) {
                    budgetAmount = ((Number) req.get("budget")).intValue();
                } else if (req.containsKey("amount")) {
                    budgetAmount = ((Number) req.get("amount")).intValue();
                } else if (req.containsKey("salary")) {
                    budgetAmount = ((Number) req.get("salary")).intValue();
                }
                if (req.containsKey("month")) {
                    month = (String) req.get("month");
                } else if (req.containsKey("monthYear")) {
                    month = (String) req.get("monthYear");
                }
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (budgetAmount <= 0) {
            sendErrorResponse(exchange, 400, "Monthly budget amount must be greater than 0");
            return;
        }

        if (month == null || month.trim().isEmpty()) {
            month = YearMonth.now().toString();
        }

        MonthlyBudgetSummary summary = expenseService.saveCurrentMonthlyBudget(month, budgetAmount);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleDeleteCurrentBudget(HttpExchange exchange, URI uri) throws IOException {
        String month = getQueryParam(uri.getQuery(), "month");
        if (month == null || month.trim().isEmpty()) {
            month = YearMonth.now().toString();
        }

        MonthlyBudgetSummary summary = expenseService.deleteCurrentMonthlyBudget(month);
        sendJsonResponse(exchange, 200, gson.toJson(summary));
    }

    private void handleGetBudget(HttpExchange exchange, URI uri) throws IOException {
        String month = getQueryParam(uri.getQuery(), "month");
        if (month == null || month.trim().isEmpty()) {
            month = YearMonth.now().toString();
        }

        Map<String, Object> budget = expenseService.getBudgetGuidelines(month);
        if (budget == null) {
            // Return empty structure with default suggestion placeholder
            budget = new HashMap<>();
            budget.put("salary", 0);
            budget.put("monthYear", month);
            budget.put("totalAllocated", 0);
            budget.put("unallocated", 0);
            budget.put("guidelines", new HashMap<>());
            budget.put("isGuidelineOnly", true);
        }

        sendJsonResponse(exchange, 200, gson.toJson(budget));
    }

    private void handleSaveBudget(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        BudgetRequest req;
        try {
            req = gson.fromJson(body, BudgetRequest.class);
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (req == null || req.salary <= 0) {
            sendErrorResponse(exchange, 400, "Salary must be greater than 0");
            return;
        }

        String month = req.monthYear;
        if (month == null || month.trim().isEmpty()) {
            month = YearMonth.now().toString();
        }

        Map<String, Object> result = expenseService.saveBudgetGuidelines(month, req.salary, req.categories);
        sendJsonResponse(exchange, 200, gson.toJson(result));
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
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
