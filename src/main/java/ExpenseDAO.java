import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                exp.setId(nextId);
                System.out.println("Saved to MySQL database successfully! (ID: " + nextId + ")");
            }
        } catch (SQLException e) {
            System.out.println("Database Insert Error: " + e.getMessage());
        }
    }

    // 2. Retrieves and returns all expenses from the MySQL database
    public List<Expense> viewExpenses() {
        List<Expense> expenses = new ArrayList<>();
        String selectSql = "SELECT Id, Amount, Category, Description, date FROM expenses ORDER BY Id ASC";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot view expenses: Database connection unavailable.");
                return expenses;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    Expense exp = new Expense();
                    exp.setId(rs.getInt("Id"));
                    exp.setAmount(rs.getInt("Amount"));
                    exp.setCategory(rs.getString("Category"));
                    exp.setDescription(rs.getString("Description"));
                    exp.setDate(rs.getString("date"));
                    expenses.add(exp);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while viewing expenses: " + e.getMessage());
        }
        return expenses;
    }

    // Retrieves an expense by its ID
    public Expense getExpenseById(int id) {
        String selectSql = "SELECT Id, Amount, Category, Description, date FROM expenses WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Expense exp = new Expense();
                        exp.setId(rs.getInt("Id"));
                        exp.setAmount(rs.getInt("Amount"));
                        exp.setCategory(rs.getString("Category"));
                        exp.setDescription(rs.getString("Description"));
                        exp.setDate(rs.getString("date"));
                        return exp;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error retrieving expense by id: " + e.getMessage());
        }
        return null;
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

    // 5. Calculates and returns total expense from MySQL
    public double calculateTotalExpense() {
        String sumSql = "SELECT COALESCE(SUM(Amount), 0) AS total FROM expenses";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot calculate total: Database connection unavailable.");
                return 0.0;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sumSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while calculating total: " + e.getMessage());
        }
        return 0.0;
    }

    // 6. Calculates and returns category summary from MySQL
    public Map<String, Integer> showCategorySummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        String summarySql = "SELECT Category, SUM(Amount) AS total FROM expenses GROUP BY Category ORDER BY Category";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot retrieve summary: Database connection unavailable.");
                return summary;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(summarySql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String category = rs.getString("Category");
                    int total = rs.getInt("total");
                    summary.put(category, total);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while fetching category summary: " + e.getMessage());
        }
        return summary;
    }

    // 7. Calculates and returns spending insights from MySQL
    public Map<String, Double> showSpendingInsights() {
        Map<String, Double> insights = new LinkedHashMap<>();
        String totalSql = "SELECT COALESCE(SUM(Amount), 0) AS grand_total FROM expenses";
        String categorySql = "SELECT Category, SUM(Amount) AS total FROM expenses GROUP BY Category ORDER BY Category";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot retrieve insights: Database connection unavailable.");
                return insights;
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
                return insights;
            }

            // Step 2: Query category totals and calculate percentages
            try (PreparedStatement catStmt = conn.prepareStatement(categorySql);
                 ResultSet catRs = catStmt.executeQuery()) {

                while (catRs.next()) {
                    String category = catRs.getString("Category");
                    int categoryTotal = catRs.getInt("total");
                    double percentage = (categoryTotal / grandTotal) * 100.0;
                    insights.put(category, percentage);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while generating spending insights: " + e.getMessage());
        }
        return insights;
    }

    // =========================================================================
    // Automated Database Table Initialization (monthly_budget, weekly_budget & in_app_notifications)
    // =========================================================================
    public void initDatabase() {
        String createBudgetTable = "CREATE TABLE IF NOT EXISTS monthly_budget (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "month_year VARCHAR(7) NOT NULL UNIQUE, " +
                "salary INT NOT NULL, " +
                "allocations_json TEXT NOT NULL, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        String createWeeklyBudgetTable = "CREATE TABLE IF NOT EXISTS weekly_budget (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "week_year VARCHAR(20) NOT NULL UNIQUE, " +
                "start_date DATE NOT NULL, " +
                "end_date DATE NOT NULL, " +
                "budget DECIMAL(10,2) NOT NULL, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        String createNotificationTable = "CREATE TABLE IF NOT EXISTS in_app_notifications (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "notification_key VARCHAR(100) NOT NULL UNIQUE, " +
                "title VARCHAR(150) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "type VARCHAR(30) NOT NULL, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Notice: Could not connect to MySQL to verify tables.");
                return;
            }
            try (PreparedStatement s1 = conn.prepareStatement(createBudgetTable)) {
                s1.executeUpdate();
            }
            try (PreparedStatement s_week = conn.prepareStatement(createWeeklyBudgetTable)) {
                s_week.executeUpdate();
            }
            try (PreparedStatement s2 = conn.prepareStatement(createNotificationTable)) {
                s2.executeUpdate();
            }
            System.out.println("MySQL tables (expenses, monthly_budget, weekly_budget, in_app_notifications) verified and initialized successfully.");
        } catch (SQLException e) {
            System.out.println("Notice: Table initialization check: " + e.getMessage());
        }
    }

    // =========================================================================
    // Monthly Budget Guideline Persistence
    // =========================================================================
    public boolean saveOrUpdateBudget(String monthYear, int salary, String allocationsJson) {
        String sql = "INSERT INTO monthly_budget (month_year, salary, allocations_json) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE salary = VALUES(salary), allocations_json = VALUES(allocations_json)";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, monthYear);
                pstmt.setInt(2, salary);
                pstmt.setString(3, allocationsJson);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error saving budget: " + e.getMessage());
            return false;
        }
    }

    public boolean saveOrUpdateMonthlyBudget(String monthYear, int budgetAmount) {
        String sql = "INSERT INTO monthly_budget (month_year, salary, allocations_json) " +
                     "VALUES (?, ?, '{}') " +
                     "ON DUPLICATE KEY UPDATE salary = VALUES(salary)";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, monthYear);
                pstmt.setInt(2, budgetAmount);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error saving monthly budget: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMonthlyBudget(String monthYear) {
        String sql = "DELETE FROM monthly_budget WHERE month_year = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, monthYear);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error deleting monthly budget: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Weekly Budget Persistence
    // =========================================================================
    public boolean saveOrUpdateWeeklyBudget(String weekYear, String startDate, String endDate, double budgetAmount) {
        String sql = "INSERT INTO weekly_budget (week_year, start_date, end_date, budget) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE budget = VALUES(budget), start_date = VALUES(start_date), end_date = VALUES(end_date)";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, weekYear);
                pstmt.setString(2, startDate);
                pstmt.setString(3, endDate);
                pstmt.setDouble(4, budgetAmount);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error saving weekly budget: " + e.getMessage());
            return false;
        }
    }

    public Double getWeeklyBudget(String weekYear) {
        String sql = "SELECT budget FROM weekly_budget WHERE week_year = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, weekYear);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("budget");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error retrieving weekly budget: " + e.getMessage());
        }
        return null;
    }

    public boolean deleteWeeklyBudget(String weekYear) {
        String sql = "DELETE FROM weekly_budget WHERE week_year = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, weekYear);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error deleting weekly budget: " + e.getMessage());
            return false;
        }
    }

    public BudgetPlan getBudget(String monthYear) {
        String sql = "SELECT month_year, salary, allocations_json, updated_at FROM monthly_budget WHERE month_year = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, monthYear);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new BudgetPlan(
                            rs.getString("month_year"),
                            rs.getInt("salary"),
                            rs.getString("allocations_json"),
                            rs.getString("updated_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error retrieving budget: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Monthly Spending & Comparison Queries
    // =========================================================================
    public double getMonthlyExpensesTotal(String startDate, String endDate) {
        String sql = "SELECT COALESCE(SUM(Amount), 0) AS total FROM expenses WHERE date >= ? AND date <= ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return 0.0;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("total");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error calculating monthly expenses total: " + e.getMessage());
        }
        return 0.0;
    }

    public Map<String, Integer> getMonthlyCategoryBreakdown(String startDate, String endDate) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        String sql = "SELECT Category, SUM(Amount) AS total FROM expenses WHERE date >= ? AND date <= ? GROUP BY Category ORDER BY Category";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return summary;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        summary.put(rs.getString("Category"), rs.getInt("total"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error fetching monthly category breakdown: " + e.getMessage());
        }
        return summary;
    }

    // =========================================================================
    // In-App Notification Operations (Idempotent to avoid duplicates)
    // =========================================================================
    public boolean saveNotification(String key, String title, String message, String type) {
        // Idempotent insertion: updates message and title only if notification hasn't been marked read yet
        String sql = "INSERT INTO in_app_notifications (notification_key, title, message, type) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "message = IF(is_read = FALSE, VALUES(message), message), " +
                     "title = IF(is_read = FALSE, VALUES(title), title), " +
                     "type = IF(is_read = FALSE, VALUES(type), type)";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.setString(2, title);
                pstmt.setString(3, message);
                pstmt.setString(4, type);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error saving in-app notification: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteNotificationByKey(String key) {
        String sql = "DELETE FROM in_app_notifications WHERE notification_key = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, key);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error deleting notification: " + e.getMessage());
            return false;
        }
    }

    public List<NotificationItem> getNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        String sql = "SELECT id, notification_key, title, message, type, is_read, created_at FROM in_app_notifications ORDER BY id DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new NotificationItem(
                        rs.getInt("id"),
                        rs.getString("notification_key"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getBoolean("is_read"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error fetching notifications: " + e.getMessage());
        }
        return list;
    }

    public List<NotificationItem> getMonthlyNotifications(String monthYear) {
        List<NotificationItem> list = new ArrayList<>();
        String sql = "SELECT id, notification_key, title, message, type, is_read, created_at FROM in_app_notifications " +
                     "WHERE (notification_key LIKE 'MONTHLY_BUDGET_%' OR type LIKE 'MONTHLY_%') " +
                     "AND notification_key LIKE ? ORDER BY id DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + monthYear + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new NotificationItem(
                            rs.getInt("id"),
                            rs.getString("notification_key"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getString("type"),
                            rs.getBoolean("is_read"),
                            rs.getString("created_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error fetching monthly notifications: " + e.getMessage());
        }
        return list;
    }

    public boolean markNotificationAsRead(int id) {
        String sql = "UPDATE in_app_notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    public boolean markAllNotificationsAsRead() {
        String sql = "UPDATE in_app_notifications SET is_read = TRUE WHERE is_read = FALSE";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                return pstmt.executeUpdate() >= 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error marking all notifications as read: " + e.getMessage());
            return false;
        }
    }
}
