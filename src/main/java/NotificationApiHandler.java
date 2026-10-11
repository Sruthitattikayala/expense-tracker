import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationApiHandler implements HttpHandler {

    private final ExpenseService expenseService;
    private final Gson gson;

    public NotificationApiHandler(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            sendEmptyResponse(exchange, 204);
            return;
        }

        try {
            // GET /api/notifications/monthly
            if (path.equals("/api/notifications/monthly") || path.equals("/api/notifications/monthly/")) {
                if ("GET".equals(method)) {
                    String month = getQueryParam(exchange.getRequestURI().getQuery(), "month");
                    if (month == null || month.trim().isEmpty()) {
                        month = YearMonth.now().toString();
                    }
                    expenseService.checkAndGenerateMonthlyBudgetNotifications(month);
                    List<NotificationItem> list = expenseService.getMonthlyNotifications(month);
                    sendJsonResponse(exchange, 200, gson.toJson(list));
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // POST /api/notifications/monthly/evaluate
            if (path.equals("/api/notifications/monthly/evaluate") || path.equals("/api/notifications/monthly/evaluate/")) {
                if ("POST".equals(method)) {
                    String month = getQueryParam(exchange.getRequestURI().getQuery(), "month");
                    if (month == null || month.trim().isEmpty()) {
                        month = YearMonth.now().toString();
                    }
                    expenseService.checkAndGenerateMonthlyBudgetNotifications(month);
                    List<NotificationItem> list = expenseService.getMonthlyNotifications(month);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("month", month);
                    resp.put("notifications", list);
                    sendJsonResponse(exchange, 200, gson.toJson(resp));
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // POST /api/notifications/read-all or /api/notifications/mark-all-read
            if (path.equals("/api/notifications/read-all") || path.equals("/api/notifications/mark-all-read")) {
                if ("POST".equals(method)) {
                    boolean ok = expenseService.markAllNotificationsRead();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", ok);
                    resp.put("message", "All notifications marked as read");
                    sendJsonResponse(exchange, 200, gson.toJson(resp));
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // GET /api/notifications
            if (path.equals("/api/notifications") || path.equals("/api/notifications/")) {
                if ("GET".equals(method)) {
                    String currentMonth = YearMonth.now().toString();
                    // Trigger both weekly pacing and monthly budget evaluations
                    expenseService.checkAndGenerateWeeklyPacing(currentMonth);
                    expenseService.checkAndGenerateMonthlyBudgetNotifications(currentMonth);
                    List<NotificationItem> notifications = expenseService.getInAppNotifications();
                    sendJsonResponse(exchange, 200, gson.toJson(notifications));
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            // POST /api/notifications/{id}/read
            if (path.startsWith("/api/notifications/") && path.endsWith("/read")) {
                String middle = path.substring("/api/notifications/".length(), path.length() - "/read".length());
                try {
                    int id = Integer.parseInt(middle);
                    if ("POST".equals(method)) {
                        boolean updated = expenseService.markNotificationRead(id);
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("success", updated);
                        resp.put("id", id);
                        sendJsonResponse(exchange, 200, gson.toJson(resp));
                    } else {
                        sendErrorResponse(exchange, 405, "Method Not Allowed");
                    }
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid notification ID");
                }
                return;
            }

            sendErrorResponse(exchange, 404, "Endpoint Not Found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
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
}
