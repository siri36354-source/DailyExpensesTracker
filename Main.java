import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// Runs the application menu and reads user input.
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String[] CATEGORIES = {"Food", "Travel", "Education", "Shopping", "Other"};

    public static void main(String[] args) {
        ExpenseManager manager;
        try {
            manager = new ExpenseManager(FileStorage.loadExpenses(), FileStorage.loadBudget());
            System.out.println("Loaded " + manager.getExpenses().size() + " expense(s).");
        } catch (IOException ex) {
            System.out.println("Could not load saved data: " + ex.getMessage());
            manager = new ExpenseManager(List.of(), 0);
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=== Daily Expenses Tracker ===");
            System.out.println("1. Add expense\n2. View all expenses\n3. View monthly summary\n4. Set monthly budget\n5. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> addExpense(manager);
                case "2" -> viewExpenses(manager);
                case "3" -> showSummary(manager);
                case "4" -> setBudget(manager);
                case "5", "exit", "quit" -> running = false;
                default -> System.out.println("Invalid choice. Enter a number from 1 to 5.");
            }
        }
        saveAll(manager);
        System.out.println("Data saved. Goodbye!");
        scanner.close();
    }

    private static void addExpense(ExpenseManager manager) {
        double amount = readPositiveAmount("Amount (₹): ");
        System.out.println("Categories:");
        for (int i = 0; i < CATEGORIES.length; i++) System.out.println((i + 1) + ". " + CATEGORIES[i]);
        System.out.print("Choose category (1-5): ");
        int categoryChoice = readIntInRange(1, CATEGORIES.length);
        String category = CATEGORIES[categoryChoice - 1];
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) description = "No description";

        LocalDate date;
        while (true) {
            System.out.print("Date (YYYY-MM-DD, blank for today): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) date = LocalDate.now();
            else {
                try { date = LocalDate.parse(input); }
                catch (DateTimeParseException ex) { System.out.println("Invalid date format. Try YYYY-MM-DD."); continue; }
            }
            if (!ExpenseManager.isValidDate(date)) {
                System.out.println("Date cannot be in the future.");
                continue;
            }
            break;
        }
        manager.addExpense(new Expense(amount, category, description, date));
        saveAll(manager);
        System.out.println("Expense added successfully.");
    }

    private static void viewExpenses(ExpenseManager manager) {
        List<Expense> expenses = manager.getExpenses();
        if (expenses.isEmpty()) { System.out.println("No expenses recorded yet."); return; }
        System.out.println("\nDate         | Category     | Amount       | Description");
        System.out.println("---------------------------------------------------------------");
        for (Expense expense : expenses) System.out.println(expense);
    }

    private static void showSummary(ExpenseManager manager) {
        YearMonth month = YearMonth.now();
        double total = manager.getTotalForMonth(month);
        System.out.println("\nMonthly summary for " + month);
        System.out.printf("Total spending: ₹%.2f%n", total);
        System.out.println("Spending by category:");
        Map<String, Double> totals = manager.getCategoryTotals(month);
        if (totals.isEmpty()) System.out.println("No expenses for this month.");
        else totals.forEach((category, amount) -> System.out.printf("- %s: ₹%.2f%n", category, amount));
        if (manager.getMonthlyBudget() > 0) {
            double remaining = manager.getRemainingBudget(month);
            if (remaining >= 0) System.out.printf("Budget remaining: ₹%.2f%n", remaining);
            else System.out.printf("Budget exceeded by: ₹%.2f%n", -remaining);
        } else System.out.println("No monthly budget set yet.");
    }

    private static void setBudget(ExpenseManager manager) {
        double budget = readPositiveAmount("Enter monthly budget (₹): ");
        manager.setMonthlyBudget(budget);
        try { FileStorage.saveBudget(budget); System.out.println("Monthly budget saved."); }
        catch (IOException ex) { System.out.println("Could not save budget: " + ex.getMessage()); }
    }

    private static double readPositiveAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double amount = Double.parseDouble(scanner.nextLine().trim());
                if (ExpenseManager.isValidAmount(amount)) return amount;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a valid number greater than zero (example: 125.50).");
        }
    }

    private static int readIntInRange(int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.print("Enter a number from " + min + " to " + max + ": ");
        }
    }

    private static void saveAll(ExpenseManager manager) {
        try { FileStorage.saveExpenses(manager.getExpenses()); }
        catch (IOException ex) { System.out.println("Could not save expenses: " + ex.getMessage()); }
    }
}
