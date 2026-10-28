import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Contains expense operations and calculations, separate from the menu.
public class ExpenseManager {
    private final List<Expense> expenses = new ArrayList<>();
    private double monthlyBudget;

    public ExpenseManager(List<Expense> savedExpenses, double savedBudget) {
        expenses.addAll(savedExpenses);
        monthlyBudget = savedBudget;
    }

    public void addExpense(Expense expense) { expenses.add(expense); }
    public List<Expense> getExpenses() { return new ArrayList<>(expenses); }
    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double budget) { monthlyBudget = budget; }

    public double getTotalForMonth(YearMonth month) {
        double total = 0;
        for (Expense e : expenses) {
            if (YearMonth.from(e.getDate()).equals(month)) total += e.getAmount();
        }
        return total;
    }

    public Map<String, Double> getCategoryTotals(YearMonth month) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Expense e : expenses) {
            if (YearMonth.from(e.getDate()).equals(month)) {
                totals.put(e.getCategory(), totals.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
            }
        }
        return totals;
    }

    public double getRemainingBudget(YearMonth month) {
        return monthlyBudget - getTotalForMonth(month);
    }

    public static boolean isValidAmount(double amount) { return Double.isFinite(amount) && amount > 0; }
    public static boolean isValidDate(LocalDate date) { return date != null && !date.isAfter(LocalDate.now()); }
}
