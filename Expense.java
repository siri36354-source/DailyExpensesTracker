import java.time.LocalDate;

// Represents one expense record.
public class Expense {
    private final double amount;
    private final String category;
    private final String description;
    private final LocalDate date;

    public Expense(double amount, String category, String description, LocalDate date) {
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return String.format("%-12s | %-12s | ₹%8.2f | %s", date, category, amount, description);
    }
}
