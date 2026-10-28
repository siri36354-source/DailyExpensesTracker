import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.BindException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Lightweight local web server using only the JDK. The existing console app remains unchanged.
public class WebMain {
    private static final int PORT = 8080;
    private static final Path WEB_DIR = Path.of("web").toAbsolutePath().normalize();
    private static final String[] CATEGORIES = {"Food", "Travel", "Education", "Shopping", "Other"};
    private static ExpenseManager manager;

    public static void main(String[] args) throws IOException {
        manager = new ExpenseManager(FileStorage.loadExpenses(), FileStorage.loadBudget());
        int requestedPort = Integer.getInteger("port", PORT);
        HttpServer server = null;
        int port = requestedPort;
        // If the default port is already in use, try the next few ports automatically.
        for (int attempt = 0; attempt < 10; attempt++) {
            port = requestedPort + attempt;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                break;
            } catch (BindException ex) {
                if (attempt == 9) throw ex;
            }
        }
        server.createContext("/api/expenses", WebMain::handleExpenses);
        server.createContext("/api/budget", WebMain::handleBudget);
        server.createContext("/api/summary", WebMain::handleSummary);
        server.createContext("/", WebMain::handleStaticFiles);
        server.setExecutor(null);
        server.start();
        System.out.println("Daily Expenses Tracker website is running at http://localhost:" + port);
        System.out.println("Keep this terminal open while using the website. Press Ctrl+C to stop it.");
    }

    private static synchronized void handleExpenses(HttpExchange exchange) throws IOException {
        addCommonHeaders(exchange);
        if ("GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 200, expensesJson(manager.getExpenses()));
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Map<String, String> form = readForm(exchange);
            double amount = Double.parseDouble(form.getOrDefault("amount", ""));
            String category = form.getOrDefault("category", "").trim();
            String description = form.getOrDefault("description", "").trim();
            LocalDate date = LocalDate.parse(form.getOrDefault("date", ""));
            if (!ExpenseManager.isValidAmount(amount)) throw new IllegalArgumentException("Enter an amount greater than zero.");
            if (!isCategory(category)) throw new IllegalArgumentException("Choose a valid category.");
            if (description.isBlank()) throw new IllegalArgumentException("Enter a description.");
            if (description.contains("|") || description.contains("\n") || description.contains("\r")) {
                description = description.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
            }
            if (!ExpenseManager.isValidDate(date)) throw new IllegalArgumentException("Date cannot be in the future.");
            manager.addExpense(new Expense(amount, category, description, date));
            FileStorage.saveExpenses(manager.getExpenses());
            sendJson(exchange, 201, "{\"message\":\"Expense saved successfully.\"}");
        } catch (NumberFormatException | DateTimeParseException ex) {
            sendJson(exchange, 400, "{\"error\":\"Enter a valid amount and date.\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":" + jsonString(ex.getMessage()) + "}");
        } catch (IOException ex) {
            sendJson(exchange, 500, "{\"error\":\"Could not save expense to file.\"}");
        }
    }

    private static synchronized void handleBudget(HttpExchange exchange) throws IOException {
        addCommonHeaders(exchange);
        if ("GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 200, "{\"budget\":" + manager.getMonthlyBudget() + "}");
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Map<String, String> form = readForm(exchange);
            double budget = Double.parseDouble(form.getOrDefault("budget", ""));
            if (!ExpenseManager.isValidAmount(budget)) throw new IllegalArgumentException("Enter a budget greater than zero.");
            manager.setMonthlyBudget(budget);
            FileStorage.saveBudget(budget);
            sendJson(exchange, 200, "{\"message\":\"Monthly budget saved.\",\"budget\":" + budget + "}");
        } catch (NumberFormatException ex) {
            sendJson(exchange, 400, "{\"error\":\"Enter a valid budget amount.\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":" + jsonString(ex.getMessage()) + "}");
        } catch (IOException ex) {
            sendJson(exchange, 500, "{\"error\":\"Could not save budget to file.\"}");
        }
    }

    private static synchronized void handleSummary(HttpExchange exchange) throws IOException {
        addCommonHeaders(exchange);
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        YearMonth month = YearMonth.now();
        double spent = manager.getTotalForMonth(month);
        double budget = manager.getMonthlyBudget();
        Map<String, Double> categories = manager.getCategoryTotals(month);
        StringBuilder json = new StringBuilder("{\"month\":").append(jsonString(month.toString()))
                .append(",\"monthlySpending\":").append(spent)
                .append(",\"budget\":").append(budget)
                .append(",\"remainingBudget\":").append(budget - spent)
                .append(",\"totalExpenses\":").append(manager.getExpenses().stream().mapToDouble(Expense::getAmount).sum())
                .append(",\"categoryTotals\":{");
        boolean first = true;
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            if (!first) json.append(',');
            json.append(jsonString(entry.getKey())).append(':').append(entry.getValue());
            first = false;
        }
        json.append("}}");
        sendJson(exchange, 200, json.toString());
    }

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = WEB_DIR.resolve(path.substring(1)).normalize();
        if (!file.startsWith(WEB_DIR) || !Files.isRegularFile(file)) {
            byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
            return;
        }
        String type = file.toString().endsWith(".css") ? "text/css; charset=utf-8"
                : file.toString().endsWith(".js") ? "application/javascript; charset=utf-8"
                : "text/html; charset=utf-8";
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        byte[] body = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) { out.write(body); }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String expensesJson(List<Expense> expenses) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            if (i > 0) json.append(',');
            json.append("{\"amount\":").append(e.getAmount())
                    .append(",\"category\":").append(jsonString(e.getCategory()))
                    .append(",\"description\":").append(jsonString(e.getDescription()))
                    .append(",\"date\":").append(jsonString(e.getDate().toString())).append('}');
        }
        return json.append(']').toString();
    }

    private static boolean isCategory(String category) {
        for (String valid : CATEGORIES) if (valid.equals(category)) return true;
        return false;
    }

    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (var out = exchange.getResponseBody()) { out.write(body); }
    }
}
