import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AiApiHandler implements HttpHandler {

    private final Gson gson = new Gson();

    public static class ParseRequest {
        public String text;
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
            if (path.startsWith("/api/ai/parse") || path.startsWith("/api/ai")) {
                if ("POST".equals(method)) {
                    handleParse(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                }
                return;
            }

            sendErrorResponse(exchange, 404, "Endpoint Not Found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleParse(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        ParseRequest req;
        try {
            req = gson.fromJson(body, ParseRequest.class);
        } catch (Exception e) {
            sendErrorResponse(exchange, 400, "Malformed JSON request body");
            return;
        }

        if (req == null || req.text == null || req.text.trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Field 'text' cannot be empty");
            return;
        }

        AiParserService.AiParseResult parseResult = AiParserService.getInstance().parse(req.text);
        sendJsonResponse(exchange, 200, gson.toJson(parseResult));
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
        headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
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
        headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
