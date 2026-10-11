import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StaticFileHandler implements HttpHandler {

    private static final String CLASSPATH_PREFIX = "/static";
    private static final String FILE_FALLBACK_DIR = "src/main/resources/static";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        // Normalize root path to index.html
        if (path == null || path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }

        // Prevent directory traversal
        if (path.contains("..")) {
            sendNotFound(exchange);
            return;
        }

        // 1. Try reading from classpath (JAR or target/classes)
        byte[] content = loadFromClasspath(CLASSPATH_PREFIX + path);

        // 2. Fallback: try reading directly from filesystem (dev mode)
        if (content == null) {
            content = loadFromFile(FILE_FALLBACK_DIR + path);
        }

        // 3. Fallback: if path has no file extension, try resolving as .html
        if (content == null && !path.contains(".")) {
            content = loadFromClasspath(CLASSPATH_PREFIX + path + ".html");
            if (content == null) {
                content = loadFromFile(FILE_FALLBACK_DIR + path + ".html");
            }
            if (content != null) {
                path = path + ".html";
            }
        }

        if (content == null) {
            sendNotFound(exchange);
            return;
        }

        String contentType = getContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);

        if ("GET".equals(method)) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
                os.flush();
            }
        } else {
            exchange.close();
        }
    }

    private byte[] loadFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
            String clPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            try (InputStream is2 = getClass().getClassLoader().getResourceAsStream(clPath)) {
                if (is2 != null) {
                    return is2.readAllBytes();
                }
            }
        } catch (IOException e) {
            // Ignore and fallback
        }
        return null;
    }

    private byte[] loadFromFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return fis.readAllBytes();
            } catch (IOException e) {
                // Ignore
            }
        }
        return null;
    }

    private String getContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        } else if (path.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (path.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (path.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (path.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found";
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }
}
