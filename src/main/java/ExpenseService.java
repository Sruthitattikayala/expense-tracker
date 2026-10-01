public class ExpenseService {

    private final ExpenseDAO expenseDAO;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    // Coordinates checking whether any expenses exist
    public boolean hasExpenses() {
        return expenseDAO.hasExpenses();
    }

    // Coordinates checking whether a specific expense ID exists
    public boolean expenseExists(int id) {
        return expenseDAO.expenseExists(id);
    }

    // Business operation: Add a new expense
    public void addExpense(Expense exp) {
        expenseDAO.addExpense(exp);
    }

    // Business operation: View all expenses
    public void viewExpenses() {
        expenseDAO.viewExpenses();
    }

    // Business operation: Delete an expense by ID
    public boolean deleteExpense(int deleteNumber) {
        return expenseDAO.deleteExpense(deleteNumber);
    }

    // Business operation: Update an existing expense by ID
    public boolean updateExpense(int modifyNumber, int amount, String category, String description, String date) {
        return expenseDAO.updateExpense(modifyNumber, amount, category, description, date);
    }

    // Business operation: Calculate and display total expense
    public void calculateTotalExpense() {
        expenseDAO.calculateTotalExpense();
    }

    // Business operation: Display category summary
    public void showCategorySummary() {
        expenseDAO.showCategorySummary();
    }

    // Business operation: Display spending insights
    public void showSpendingInsights() {
        expenseDAO.showSpendingInsights();
    }
}
