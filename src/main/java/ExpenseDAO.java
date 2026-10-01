import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseDAO {

    // Checks if the database has any expense records
    public boolean hasExpenses() {
        String countSql = "SELECT COUNT(*) FROM expenses";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement stmt = conn.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // Checks if a specific expense ID exists in the database
    public boolean expenseExists(int id) {
        String selectSql = "SELECT Id FROM expenses WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // Helper: Finds the next available Id (MAX(Id) + 1)
    private int getNextId(Connection conn) {
        String query = "SELECT COALESCE(MAX(Id), 0) + 1 FROM expenses";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Could not generate next ID: " + e.getMessage());
        }
        return 1;
    }

    // 1. Adds a new expense into the MySQL expenses table
    public void addExpense(Expense exp) {
        String insertSql = "INSERT INTO expenses (Id, Amount, Category, Description, date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot add expense: Database connection unavailable.");
                return;
            }
            int nextId = getNextId(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, nextId);
                pstmt.setInt(2, exp.getAmount());
                pstmt.setString(3, exp.getCategory());
                pstmt.setString(4, exp.getDescription());
                pstmt.setDate(5, Date.valueOf(exp.getDate()));

                pstmt.executeUpdate();
                System.out.println("Saved to MySQL database successfully! (ID: " + nextId + ")");
            }
        } catch (SQLException e) {
            System.out.println("Database Insert Error: " + e.getMessage());
        }
    }

    // 2. Retrieves and displays all expenses from the MySQL database
    public void viewExpenses() {
        String selectSql = "SELECT Id, Amount, Category, Description, date FROM expenses ORDER BY Id ASC";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot view expenses: Database connection unavailable.");
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql);
                 ResultSet rs = pstmt.executeQuery()) {

                int i = 1;
                boolean hasRecords = false;
                while (rs.next()) {
                    hasRecords = true;
                    int amount = rs.getInt("Amount");
                    String category = rs.getString("Category");
                    String description = rs.getString("Description");
                    String date = rs.getString("date");

                    System.out.println("\n----- Expense -----");
                    System.out.println(i + ". Amount: ₹" + amount);
                    i++;
                    System.out.println("Category: " + category);
                    System.out.println("Description: " + description);
                    System.out.println("Date: " + date);
                }

                if (!hasRecords) {
                    System.out.println("\n\nEnter the details first:");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while viewing expenses: " + e.getMessage());
        }
    }

    // 3. Deletes an expense from the MySQL database by Id
    public boolean deleteExpense(int deleteNumber) {
        String deleteSql = "DELETE FROM expenses WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot delete: Database connection unavailable.");
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, deleteNumber);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected == 1;
            }
        } catch (SQLException e) {
            System.out.println("Database error while deleting expense: " + e.getMessage());
            return false;
        }
    }

    // 4. Updates an existing expense in the MySQL database by Id
    public boolean updateExpense(int modifyNumber, int amount, String category, String description, String date) {
        String updateSql = "UPDATE expenses SET Amount = ?, Category = ?, Description = ?, date = ? WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot update: Database connection unavailable.");
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, amount);
                pstmt.setString(2, category);
                pstmt.setString(3, description);
                pstmt.setDate(4, Date.valueOf(date));
                pstmt.setInt(5, modifyNumber);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected == 1;
            }
        } catch (SQLException e) {
            System.out.println("Database error while updating expense: " + e.getMessage());
            return false;
        }
    }

    // 5. Calculates and displays total expense from MySQL
    public void calculateTotalExpense() {
        if (!hasExpenses()) {
            System.out.println("\n\nEnter the details first:");
            return;
        }

        String sumSql = "SELECT COALESCE(SUM(Amount), 0) AS total FROM expenses";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot calculate total: Database connection unavailable.");
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sumSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    System.out.println("Total Expenses : Rs. " + total);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while calculating total: " + e.getMessage());
        }
    }

    // 6. Calculates and displays category summary from MySQL
    public void showCategorySummary() {
        String summarySql = "SELECT Category, SUM(Amount) AS total FROM expenses GROUP BY Category ORDER BY Category";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot retrieve summary: Database connection unavailable.");
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(summarySql);
                 ResultSet rs = pstmt.executeQuery()) {

                boolean hasRecords = false;
                while (rs.next()) {
                    if (!hasRecords) {
                        System.out.println("\n----- Category Summary -----");
                        hasRecords = true;
                    }
                    String category = rs.getString("Category");
                    int total = rs.getInt("total");
                    System.out.println(category + " : ₹" + total);
                }

                if (!hasRecords) {
                    System.out.println("\nEnter the details first:");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while fetching category summary: " + e.getMessage());
        }
    }

    // 7. Calculates and displays spending insights from MySQL
    public void showSpendingInsights() {
        String totalSql = "SELECT COALESCE(SUM(Amount), 0) AS grand_total FROM expenses";
        String categorySql = "SELECT Category, SUM(Amount) AS total FROM expenses GROUP BY Category ORDER BY Category";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot retrieve insights: Database connection unavailable.");
                return;
            }

            // Step 1: Calculate overall total spending
            double grandTotal = 0;
            try (PreparedStatement totalStmt = conn.prepareStatement(totalSql);
                 ResultSet totalRs = totalStmt.executeQuery()) {
                if (totalRs.next()) {
                    grandTotal = totalRs.getDouble("grand_total");
                }
            }

            // Handle the case where there are no expenses
            if (grandTotal == 0) {
                System.out.println("\nEnter the details first:");
                return;
            }

            // Step 2: Query category totals and calculate percentages
            try (PreparedStatement catStmt = conn.prepareStatement(categorySql);
                 ResultSet catRs = catStmt.executeQuery()) {

                System.out.println("\n----- Spending Insights -----");
                while (catRs.next()) {
                    String category = catRs.getString("Category");
                    int categoryTotal = catRs.getInt("total");
                    double percentage = (categoryTotal / grandTotal) * 100.0;

                    System.out.printf("%s : ₹%d (%.2f%% of total)%n", category, categoryTotal, percentage);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while generating spending insights: " + e.getMessage());
        }
    }
}
