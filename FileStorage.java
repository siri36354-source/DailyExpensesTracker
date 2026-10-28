import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Handles all reading and writing to local files.
public class FileStorage {
    private static final Path EXPENSE_FILE = Path.of("expenses.csv");
    private static final Path BUDGET_FILE = Path.of("budget.txt");

    public static void saveExpenses(List<Expense> expenses) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("amount|category|description|date");
        for (Expense e : expenses) {
            // A pipe is used as the separator; user-entered pipes/newlines are cleaned first.
            String description = clean(e.getDescription());
            lines.add(e.getAmount() + "|" + clean(e.getCategory()) + "|" + description + "|" + e.getDate());
        }
        Files.write(EXPENSE_FILE, lines);
    }

    public static List<Expense> loadExpenses() throws IOException {
        List<Expense> expenses = new ArrayList<>();
        if (!Files.exists(EXPENSE_FILE)) return expenses;

        List<String> lines = Files.readAllLines(EXPENSE_FILE);
        for (int i = 1; i < lines.size(); i++) { // Skip the header row.
            String[] parts = lines.get(i).split("\\|", 4);
            if (parts.length != 4) continue;
            try {
                expenses.add(new Expense(Double.parseDouble(parts[0]), parts[1], parts[2], LocalDate.parse(parts[3])));
            } catch (RuntimeException ex) {
                System.out.println("Skipped an invalid record in expenses.csv (line " + (i + 1) + ").");
            }
        }
        return expenses;
    }

    public static void saveBudget(double budget) throws IOException {
        Files.writeString(BUDGET_FILE, Double.toString(budget));
    }

    public static double loadBudget() throws IOException {
        if (!Files.exists(BUDGET_FILE)) return 0.0;
        try {
            return Double.parseDouble(Files.readString(BUDGET_FILE).trim());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static String clean(String value) {
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
