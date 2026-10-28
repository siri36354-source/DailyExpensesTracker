# Daily Expenses Tracker (Core Java)

A beginner-friendly console application to record expenses, review spending by category, and compare monthly spending with a budget. It uses only the Java standard library—no frameworks or external libraries.

## Files
- `Main.java`: menu, input validation, and user interaction.
- `Expense.java`: data class representing one expense.
- `ExpenseManager.java`: stores expense records in an `ArrayList` and calculates totals.
- `FileStorage.java`: saves and loads records and the monthly budget.
- `expenses.csv`: created automatically when the program saves expenses.
- `budget.txt`: created automatically when a budget is saved.

## Requirements
Install a JDK (Java 17 or newer recommended) and Visual Studio Code with the **Extension Pack for Java**.

## Run in VS Code on Windows
1. Open VS Code.
2. Select **File → Open Folder** and choose `DailyExpensesTracker`.
3. Open **Terminal → New Terminal**.
4. Confirm Java is installed by running `java -version` and `javac -version`.
5. Compile the program:
   ```powershell
   javac Main.java Expense.java ExpenseManager.java FileStorage.java
   ```
6. Run it:
   ```powershell
   java Main
   ```
7. Use menu options 1–4. Choose 5 to exit. Saved files are created in the current working directory (normally the project folder when using the terminal steps above).

## Storage format
`expenses.csv` is a simple pipe-separated text file with a header. The program uses this format to avoid needing a JSON library. `budget.txt` stores the monthly budget. Both files are local to the project and persist between runs. In descriptions/categories, `|` is replaced with `/` and line breaks with spaces so records remain readable.

## Example session
```text
Loaded 0 expense(s).
=== Daily Expenses Tracker ===
1. Add expense
2. View all expenses
3. View monthly summary
4. Set monthly budget
5. Exit
Choose an option: 4
Enter monthly budget (₹): 5000
Monthly budget saved.
Choose an option: 1
Amount (₹): 150
Categories:
1. Food
2. Travel
3. Education
4. Shopping
5. Other
Choose category (1-5): 1
Description: Lunch
Date (YYYY-MM-DD, blank for today):
Expense added successfully.
Choose an option: 3
Monthly summary for <current year-month>
Total spending: ₹150.00
Spending by category:
- Food: ₹150.00
Budget remaining: ₹4850.00
Choose an option: 5
Data saved. Goodbye!
```
The displayed month and date depend on when you run the program. The exact `Loaded` count depends on saved data from earlier runs.

## Calculation rules
- Only expenses dated in the current month are included in the monthly summary and budget calculation.
- Expense amounts must be greater than zero.
- Dates must use `YYYY-MM-DD` and cannot be in the future.
- If spending exceeds the budget, the summary shows the amount exceeded.

## Upload to GitHub
1. Create a new empty repository on GitHub, for example `DailyExpensesTracker`.
2. Open the project folder in VS Code and open its terminal.
3. Run:
   ```powershell
   git init
   git add Main.java Expense.java ExpenseManager.java FileStorage.java README.md
   git commit -m "Build daily expenses tracker in Java"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/DailyExpensesTracker.git
   git push -u origin main
   ```
4. Replace `YOUR-USERNAME` with your GitHub username. If Git asks you to sign in, follow the browser authentication prompt.
5. Do not commit personal expense data. The commands above intentionally add source files and README only; `expenses.csv` and `budget.txt` remain local.

## Suggested manual tests
1. Enter `-20` as an amount: it should reject the input and ask again.
2. Add expenses in two categories: the monthly category report should show each category's sum.
3. Set a budget and add an expense: remaining budget should equal budget minus current-month expenses.
4. Exit and run `java Main` again: the saved expense count and records should load.
5. Enter a future date or invalid date format: it should be rejected.

## Run the website version (HTML, CSS, JavaScript + Java)

The project now also includes a browser-based green dashboard. It uses the existing `Expense`, `ExpenseManager`, and `FileStorage` classes. The original terminal app is still available; this web version is started separately with `WebMain`.

### Start it in Windows (recommended)

1. Extract the ZIP file.
2. Open the extracted `DailyExpensesTracker` project folder.
3. Double-click **`run-website.bat`**. It compiles the Java files and starts the local server from the correct folder.
4. In the terminal window, read the exact URL printed by Java (normally `http://localhost:8080`) and open that URL in your browser.
5. Keep the terminal window open while using the website. Press `Ctrl+C` to stop it.

**Do not double-click `web/index.html` or open it with Live Server.** The website calls the Java backend, so it must be opened through the URL printed by `WebMain`. If port 8080 is already in use, the server automatically tries the next port and prints the correct URL.

### Start it manually in VS Code

From the project root (the folder containing `Main.java` and `WebMain.java`), run:

```powershell
javac Main.java Expense.java ExpenseManager.java FileStorage.java WebMain.java
java WebMain
```

Then open the exact localhost URL printed in the terminal. Keep the terminal open while using the website.

### Website features

- Green responsive dashboard with monthly spending, remaining budget, and total transaction count.
- Add expenses with amount, category, description, and date validation.
- Search expenses and filter by category.
- Pie chart for current-month category spending.
- Set or update the monthly budget from the Remaining Budget card.
- Uses the same `expenses.csv` and `budget.txt` files as the terminal app, so the saved data is shared between both versions.

### Notes

- Requires a JDK that supports `jdk.httpserver` (JDK 17 or newer is recommended).
- No Spring Boot, npm, Node.js, or external Java libraries are required.
- Run commands from the project root so the website files and local data files are found correctly.
- The web server binds to `127.0.0.1` only, so it is intended for local use on your computer, not public hosting.
- The website's HTTP server is implemented with the JDK built-in `HttpServer`; the frontend files are in the `web` folder.
