import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Requirements {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ExpenseService expenseService = new ExpenseService();
        int choice = 0;

        // Step 1: Verify MySQL JDBC Connection on startup
        System.out.println("Connecting to database...");
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            System.out.println("Successfully connected to MySQL database: Expense_Tracker\n");
            try {
                conn.close();
            } catch (SQLException e) {
                // connection cleanup
            }
        } else {
            System.out.println("Notice: Could not connect to MySQL.\n");
        }

        while (choice != 8) {
            displayMenu();
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    addExpense(sc, expenseService);
                    break;
                case 2:
                    viewExpenses(expenseService);
                    break;
                case 3:
                    deleteExpense(sc, expenseService);
                    break;
                case 4:
                    updateExpense(sc, expenseService);
                    break;
                case 5:
                    calculateTotalExpense(expenseService);
                    break;
                case 6:
                    showCategorySummary(expenseService);
                    break;
                case 7:
                    showSpendingInsights(expenseService);
                    break;
                case 8:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice!");
            }
        }
        sc.close();
    }

    // Displays the main menu options
    private static void displayMenu() {
        System.out.println("\n1. Add Expense");
        System.out.println("2. View Expense");
        System.out.println("3. Delete Expense");
        System.out.println("4. Update Expense");
        System.out.println("5. Total Expense");
        System.out.println("6. Category Summary");
        System.out.println("7. Spending Insights");
        System.out.println("8. Exit");
        System.out.print("Choose the option: ");
    }

    // Handles user input for adding a new expense and calls Service
    private static void addExpense(Scanner sc, ExpenseService expenseService) {
        Expense exp = new Expense();

        int amount = readPositiveAmount(sc, "Amount: ");
        exp.setAmount(amount);

        String category = readNonEmptyString(sc, "Category: ", "Category");
        exp.setCategory(category);

        String description = readNonEmptyString(sc, "Description: ", "Description");
        exp.setDescription(description);

        String date = readValidDate(sc, "Date (YYYY-MM-DD): ");
        exp.setDate(date);

        // Delegate business operation to Service
        expenseService.addExpense(exp);

        System.out.println("Expense added!");
    }

    // Delegates viewing expenses to Service
    private static void viewExpenses(ExpenseService expenseService) {
        expenseService.viewExpenses();
    }

    // Handles user input for deleting an expense and calls Service
    private static void deleteExpense(Scanner sc, ExpenseService expenseService) {
        if (!expenseService.hasExpenses()) {
            System.out.println("\n\nEnter the details first:");
            return;
        }

        System.out.println("Which expense have to be Deleted:");
        int deleteNumber = sc.nextInt();
        sc.nextLine();

        boolean deleted = expenseService.deleteExpense(deleteNumber);
        if (deleted) {
            System.out.println("Expenses deleted:");
        } else {
            System.out.println("Enter valid Deleted Number:");
        }
    }

    // Handles user input for modifying an expense and calls Service
    private static void updateExpense(Scanner sc, ExpenseService expenseService) {
        if (!expenseService.hasExpenses()) {
            System.out.println("\n\nEnter the details first:");
            return;
        }

        System.out.println("Which expense have to be Modified:");
        int modifyNumber = sc.nextInt();
        sc.nextLine();

        if (!expenseService.expenseExists(modifyNumber)) {
            System.out.println("Enter valid Modify Number:");
            return;
        }

        int amount = readPositiveAmount(sc, "New Amount: ");
        String category = readNonEmptyString(sc, "New Category: ", "Category");
        String description = readNonEmptyString(sc, "New Description: ", "Description");
        String date = readValidDate(sc, "New Date (YYYY-MM-DD): ");

        boolean updated = expenseService.updateExpense(modifyNumber, amount, category, description, date);
        if (updated) {
            System.out.println("Expense modified successfully!");
        } else {
            System.out.println("Update failed: No record was modified.");
        }
    }

    // Delegates total calculation to Service
    private static void calculateTotalExpense(ExpenseService expenseService) {
        expenseService.calculateTotalExpense();
    }

    // Delegates category summary to Service
    private static void showCategorySummary(ExpenseService expenseService) {
        expenseService.showCategorySummary();
    }

    // Delegates spending insights to Service
    private static void showSpendingInsights(ExpenseService expenseService) {
        expenseService.showSpendingInsights();
    }

    // Helper: Prompts repeatedly until a valid positive amount is entered
    private static int readPositiveAmount(Scanner sc, String prompt) {
        System.out.print(prompt);
        int amount = 0;
        while (amount <= 0) {
            amount = sc.nextInt();
            if (amount <= 0) {
                System.out.println("Enter valid amount:");
            }
        }
        sc.nextLine();
        return amount;
    }

    // Helper: Prompts repeatedly until non-empty text is entered
    private static String readNonEmptyString(Scanner sc, String prompt, String fieldName) {
        System.out.print(prompt);
        String input = sc.nextLine();
        while (input.trim().isEmpty()) {
            System.out.println(fieldName + " cannot be empty.");
            System.out.print(prompt);
            input = sc.nextLine();
        }
        return input;
    }

    // Helper: Prompts repeatedly until a valid date format and calendar date is entered
    private static String readValidDate(Scanner sc, String prompt) {
        System.out.print(prompt);
        String date = sc.nextLine();
        while (true) {
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                System.out.println("Enter date in YYYY-MM-DD format.");
            } else {
                try {
                    LocalDate.parse(date);
                    break;
                } catch (DateTimeParseException e) {
                    System.out.println("Enter a valid date.");
                }
            }
            System.out.print(prompt);
            date = sc.nextLine();
        }
        return date;
    }
}