import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseService {

    private final ExpenseDAO expenseDAO;
    private final Gson gson;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
        this.gson = new Gson();
    }

    // Initializes database tables (monthly_budget, in_app_notifications)
    public void initDatabase() {
        expenseDAO.initDatabase();
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
        syncNotificationsForDate(exp != null ? exp.getDate() : null);
    }

    // Business operation: View all expenses
    public List<Expense> viewExpenses() {
        return expenseDAO.viewExpenses();
    }

    // Business operation: Delete an expense by ID
    public boolean deleteExpense(int deleteNumber) {
        Expense existing = expenseDAO.getExpenseById(deleteNumber);
        String dateStr = existing != null ? existing.getDate() : null;
        boolean deleted = expenseDAO.deleteExpense(deleteNumber);
        if (deleted) {
            syncNotificationsForDate(dateStr);
        }
        return deleted;
    }

    // Business operation: Update an existing expense by ID
    public boolean updateExpense(int modifyNumber, int amount, String category, String description, String date) {
        Expense existing = expenseDAO.getExpenseById(modifyNumber);
        String oldDate = existing != null ? existing.getDate() : null;
        boolean updated = expenseDAO.updateExpense(modifyNumber, amount, category, description, date);
        if (updated) {
            syncNotificationsForDate(date);
            if (oldDate != null && !oldDate.equals(date)) {
                syncNotificationsForDate(oldDate);
            }
        }
        return updated;
    }

    // Business operation: Calculate and return total expense
    public double calculateTotalExpense() {
        return expenseDAO.calculateTotalExpense();
    }

    // Business operation: Return category summary
    public Map<String, Integer> showCategorySummary() {
        return expenseDAO.showCategorySummary();
    }

    // Business operation: Return spending insights
    public Map<String, Double> showSpendingInsights() {
        return expenseDAO.showSpendingInsights();
    }

    // =========================================================================
    // Automatic Budget Distribution Engine (Guidelines Only — Never Strict Limits)
    // =========================================================================
    public Map<String, Object> calculateSuggestedBudget(int salary, List<String> categories) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("salary", salary);

        if (categories == null || categories.isEmpty()) {
            categories = Arrays.asList("Rent", "Food", "Savings", "Others");
        }

        // Relative baseline weights for categories
        Map<String, Integer> weights = new HashMap<>();
        weights.put("Rent", 30);
        weights.put("Food", 20);
        weights.put("Savings", 20);
        weights.put("Education", 15);
        weights.put("Utilities", 10);
        weights.put("Transportation", 10);
        weights.put("Healthcare", 10);
        weights.put("Gym", 5);
        weights.put("Shopping", 5);
        weights.put("Others", 10);

        int totalWeight = 0;
        for (String cat : categories) {
            totalWeight += weights.getOrDefault(cat, 10);
        }

        if (totalWeight <= 0) totalWeight = 1;

        // Allocate up to 85% proportionally, leaving ~15% unallocated cushion
        double targetPct = 85.0;
        int totalAllocated = 0;
        Map<String, Object> guidelineMap = new LinkedHashMap<>();

        for (String cat : categories) {
            int w = weights.getOrDefault(cat, 10);
            double pct = Math.round(((double) w / totalWeight) * targetPct * 10.0) / 10.0;
            int catAmount = (int) Math.round((pct / 100.0) * salary);
            totalAllocated += catAmount;

            Map<String, Object> catDetail = new LinkedHashMap<>();
            catDetail.put("percentage", pct);
            catDetail.put("amount", catAmount);
            guidelineMap.put(cat, catDetail);
        }

        int unallocated = Math.max(0, salary - totalAllocated);

        result.put("guidelines", guidelineMap);
        result.put("totalAllocated", totalAllocated);
        result.put("unallocated", unallocated);
        result.put("isGuidelineOnly", true);
        result.put("disclaimer", "These amounts are guidelines only, not strict spending limits.");

        return result;
    }

    public Map<String, Object> saveBudgetGuidelines(String monthYear, int salary, List<String> categories) {
        Map<String, Object> guidelines = calculateSuggestedBudget(salary, categories);
        guidelines.put("monthYear", monthYear);
        String json = gson.toJson(guidelines);
        expenseDAO.saveOrUpdateBudget(monthYear, salary, json);
        return guidelines;
    }

    public Map<String, Object> getBudgetGuidelines(String monthYear) {
        BudgetPlan plan = expenseDAO.getBudget(monthYear);
        if (plan != null && plan.getAllocationsJson() != null) {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(plan.getAllocationsJson(), mapType);
        }
        return null;
    }

    // =========================================================================
    // Current Month Budget Management
    // =========================================================================
    public MonthlyBudgetSummary getCurrentMonthlyBudget(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }
        YearMonth ym = YearMonth.parse(monthYear);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();

        BudgetPlan plan = expenseDAO.getBudget(monthYear);
        double budget = (plan != null) ? plan.getSalary() : 0.0;

        double totalSpent = expenseDAO.getMonthlyExpensesTotal(start, end);
        double remaining = budget > 0 ? (budget - totalSpent) : 0.0;
        double usagePct = budget > 0 ? (totalSpent / budget) * 100.0 : 0.0;

        String status;
        if (budget <= 0) {
            status = "No budget set";
        } else if (totalSpent > budget) {
            status = "Budget exceeded";
        } else if (totalSpent >= budget * 0.8) {
            status = "Near budget limit";
        } else {
            status = "Within budget";
        }

        String monthName = ym.getMonth().name().charAt(0) +
                           ym.getMonth().name().substring(1).toLowerCase() + " " + ym.getYear();

        return new MonthlyBudgetSummary(
            monthYear,
            monthName,
            budget,
            totalSpent,
            remaining,
            Math.round(usagePct * 10.0) / 10.0,
            status
        );
    }

    public MonthlyBudgetSummary saveCurrentMonthlyBudget(String monthYear, int budgetAmount) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }
        expenseDAO.saveOrUpdateMonthlyBudget(monthYear, budgetAmount);
        checkAndGenerateMonthlyBudgetNotifications(monthYear);
        return getCurrentMonthlyBudget(monthYear);
    }

    public MonthlyBudgetSummary deleteCurrentMonthlyBudget(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }
        expenseDAO.deleteMonthlyBudget(monthYear);
        checkAndGenerateMonthlyBudgetNotifications(monthYear);
        return getCurrentMonthlyBudget(monthYear);
    }

    // =========================================================================
    // Current Week Budget Management
    // =========================================================================
    public WeeklyBudgetSummary getCurrentWeeklyBudget() {
        return getCurrentWeeklyBudget(null);
    }

    public WeeklyBudgetSummary getCurrentWeeklyBudget(String weekYear) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        LocalDate startOfWeek;
        LocalDate endOfWeek;
        int weekNumber;
        int weekBasedYear;

        if (weekYear != null && weekYear.matches("\\d{4}-W\\d{2}")) {
            String[] parts = weekYear.split("-W");
            weekBasedYear = Integer.parseInt(parts[0]);
            weekNumber = Integer.parseInt(parts[1]);
            LocalDate anchor = LocalDate.of(weekBasedYear, 6, 1)
                .with(weekFields.weekBasedYear(), weekBasedYear)
                .with(weekFields.weekOfWeekBasedYear(), weekNumber);
            startOfWeek = anchor.with(weekFields.dayOfWeek(), 1); // Monday
            endOfWeek = anchor.with(weekFields.dayOfWeek(), 7);   // Sunday
        } else {
            weekNumber = today.get(weekFields.weekOfWeekBasedYear());
            weekBasedYear = today.get(weekFields.weekBasedYear());
            startOfWeek = today.with(weekFields.dayOfWeek(), 1);
            endOfWeek = today.with(weekFields.dayOfWeek(), 7);
        }

        String weekKey = String.format("%d-W%02d", weekBasedYear, weekNumber);
        String weekLabel = String.format("Week %d, %d", weekNumber, weekBasedYear);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM");
        DateTimeFormatter dtfYear = DateTimeFormatter.ofPattern("dd MMM, yyyy");
        String dateRange = startOfWeek.format(dtf) + " - " + endOfWeek.format(dtfYear);

        String startStr = startOfWeek.toString(); // "2026-09-28"
        String endStr = endOfWeek.toString();     // "2026-10-04"

        Double budgetObj = expenseDAO.getWeeklyBudget(weekKey);
        double budget = (budgetObj != null) ? budgetObj : 0.0;

        double totalSpent = expenseDAO.getMonthlyExpensesTotal(startStr, endStr);
        double remaining = budget > 0 ? (budget - totalSpent) : 0.0;
        double usagePct = budget > 0 ? (totalSpent / budget) * 100.0 : 0.0;

        String status;
        if (budget <= 0) {
            status = "No budget set";
        } else if (totalSpent > budget) {
            status = "Budget exceeded";
        } else if (totalSpent >= budget * 0.8) {
            status = "Near budget limit";
        } else {
            status = "Within budget";
        }

        return new WeeklyBudgetSummary(
            weekKey,
            weekLabel,
            startStr,
            endStr,
            dateRange,
            budget,
            totalSpent,
            remaining,
            Math.round(usagePct * 10.0) / 10.0,
            status
        );
    }

    public WeeklyBudgetSummary saveCurrentWeeklyBudget(double budgetAmount) {
        return saveCurrentWeeklyBudget(null, budgetAmount);
    }

    public WeeklyBudgetSummary saveCurrentWeeklyBudget(String weekYear, double budgetAmount) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        LocalDate startOfWeek;
        LocalDate endOfWeek;
        int weekNumber;
        int weekBasedYear;

        if (weekYear != null && weekYear.matches("\\d{4}-W\\d{2}")) {
            String[] parts = weekYear.split("-W");
            weekBasedYear = Integer.parseInt(parts[0]);
            weekNumber = Integer.parseInt(parts[1]);
            LocalDate anchor = LocalDate.of(weekBasedYear, 6, 1)
                .with(weekFields.weekBasedYear(), weekBasedYear)
                .with(weekFields.weekOfWeekBasedYear(), weekNumber);
            startOfWeek = anchor.with(weekFields.dayOfWeek(), 1);
            endOfWeek = anchor.with(weekFields.dayOfWeek(), 7);
        } else {
            weekNumber = today.get(weekFields.weekOfWeekBasedYear());
            weekBasedYear = today.get(weekFields.weekBasedYear());
            startOfWeek = today.with(weekFields.dayOfWeek(), 1);
            endOfWeek = today.with(weekFields.dayOfWeek(), 7);
        }

        String weekKey = String.format("%d-W%02d", weekBasedYear, weekNumber);
        String startStr = startOfWeek.toString();
        String endStr = endOfWeek.toString();

        expenseDAO.saveOrUpdateWeeklyBudget(weekKey, startStr, endStr, budgetAmount);
        return getCurrentWeeklyBudget(weekKey);
    }

    public WeeklyBudgetSummary deleteCurrentWeeklyBudget() {
        return deleteCurrentWeeklyBudget(null);
    }

    public WeeklyBudgetSummary deleteCurrentWeeklyBudget(String weekYear) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        int weekNumber;
        int weekBasedYear;

        if (weekYear != null && weekYear.matches("\\d{4}-W\\d{2}")) {
            String[] parts = weekYear.split("-W");
            weekBasedYear = Integer.parseInt(parts[0]);
            weekNumber = Integer.parseInt(parts[1]);
        } else {
            weekNumber = today.get(weekFields.weekOfWeekBasedYear());
            weekBasedYear = today.get(weekFields.weekBasedYear());
        }

        String weekKey = String.format("%d-W%02d", weekBasedYear, weekNumber);
        expenseDAO.deleteWeeklyBudget(weekKey);
        return getCurrentWeeklyBudget(weekKey);
    }

    // =========================================================================
    // Month-over-Month Comparison Engine
    // =========================================================================
    public Map<String, Object> getMonthlyComparison(String currentMonthYear, String previousMonthYear) {
        if (currentMonthYear == null || currentMonthYear.trim().isEmpty()) {
            currentMonthYear = YearMonth.now().toString(); // e.g. "2026-10"
        }
        if (previousMonthYear == null || previousMonthYear.trim().isEmpty()) {
            YearMonth cur = YearMonth.parse(currentMonthYear);
            previousMonthYear = cur.minusMonths(1).toString(); // e.g. "2026-09"
        }

        YearMonth curYM = YearMonth.parse(currentMonthYear);
        YearMonth prevYM = YearMonth.parse(previousMonthYear);

        String curStart = curYM.atDay(1).toString();
        String curEnd = curYM.atEndOfMonth().toString();
        String prevStart = prevYM.atDay(1).toString();
        String prevEnd = prevYM.atEndOfMonth().toString();

        double curTotal = expenseDAO.getMonthlyExpensesTotal(curStart, curEnd);
        double prevTotal = expenseDAO.getMonthlyExpensesTotal(prevStart, prevEnd);

        Map<String, Integer> curCats = expenseDAO.getMonthlyCategoryBreakdown(curStart, curEnd);
        Map<String, Integer> prevCats = expenseDAO.getMonthlyCategoryBreakdown(prevStart, prevEnd);

        double difference = curTotal - prevTotal;
        double pctChange = prevTotal > 0 ? (difference / prevTotal) * 100.0 : (curTotal > 0 ? 100.0 : 0.0);

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("currentMonth", currentMonthYear);
        comparison.put("previousMonth", previousMonthYear);
        comparison.put("currentTotal", curTotal);
        comparison.put("previousTotal", prevTotal);
        comparison.put("difference", difference);
        comparison.put("percentageChange", Math.round(pctChange * 10.0) / 10.0);
        comparison.put("currentCategories", curCats);
        comparison.put("previousCategories", prevCats);

        return comparison;
    }

    // =========================================================================
    // In-App Notification Trigger (Idempotent by unique week key)
    // =========================================================================
    public void checkAndGenerateWeeklyPacing(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }

        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int weekNum = 1;
        if (day > 21) weekNum = 4;
        else if (day > 14) weekNum = 3;
        else if (day > 7) weekNum = 2;

        String key = "WEEKLY_PACE_" + monthYear + "_W" + weekNum;

        // Calculate current spending in month
        YearMonth ym = YearMonth.parse(monthYear);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();
        double currentSpent = expenseDAO.getMonthlyExpensesTotal(start, end);

        // Calculate current week spending
        LocalDate weekStart;
        LocalDate weekEnd;
        if (weekNum == 1) {
            weekStart = ym.atDay(1);
            weekEnd = ym.atDay(Math.min(7, ym.lengthOfMonth()));
        } else if (weekNum == 2) {
            weekStart = ym.atDay(8);
            weekEnd = ym.atDay(Math.min(14, ym.lengthOfMonth()));
        } else if (weekNum == 3) {
            weekStart = ym.atDay(15);
            weekEnd = ym.atDay(Math.min(21, ym.lengthOfMonth()));
        } else {
            weekStart = ym.atDay(22);
            weekEnd = ym.atEndOfMonth();
        }
        double weekSpent = expenseDAO.getMonthlyExpensesTotal(weekStart.toString(), weekEnd.toString());

        BudgetPlan budget = expenseDAO.getBudget(monthYear);
        int salary = budget != null ? budget.getSalary() : 0;

        String title = "Week " + weekNum + " Spending Update";
        String weekSpentFormatted = String.format("%,d", (int) weekSpent);
        String currentSpentFormatted = String.format("%,d", (int) currentSpent);
        String message;

        if (salary > 0) {
            double pct = Math.round((currentSpent / salary) * 1000.0) / 10.0;
            String salaryFormatted = String.format("%,d", salary);
            message = "This week you spent ₹" + weekSpentFormatted + ". " +
                      "In Week " + weekNum + " of " + ym.getMonth().toString() +
                      ", you have spent ₹" + currentSpentFormatted + " of your ₹" + salaryFormatted +
                      " planned budget (" + pct + "%).";
            if (weekNum == 1 && pct > 35) {
                message += " You are spending faster than the recommended pace for Week 1.";
            } else if (weekNum == 2 && pct > 60) {
                message += " Consider pacing your expenses for the remaining 2 weeks.";
            } else {
                message += " Your overall spending is pacing well.";
            }
        } else {
            message = "This week you spent ₹" + weekSpentFormatted + ". Total recorded monthly spending is ₹" + currentSpentFormatted + ".";
        }

        // Idempotent insertion using UNIQUE key in MySQL
        expenseDAO.saveNotification(key, title, message, "WEEKLY_PACE");

        // Useful Category Pacing Notifications
        try {
            Map<String, Object> budgetMap = getBudgetGuidelines(monthYear);
            if (budgetMap != null && budgetMap.containsKey("guidelines")) {
                Object guidelinesObj = budgetMap.get("guidelines");
                if (guidelinesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> guidelines = (Map<String, Object>) guidelinesObj;
                    Map<String, Integer> currentMonthCats = expenseDAO.getMonthlyCategoryBreakdown(start, end);
                    for (Map.Entry<String, Object> entry : guidelines.entrySet()) {
                        String catName = entry.getKey();
                        if (entry.getValue() instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> catDetail = (Map<String, Object>) entry.getValue();
                            Object amtObj = catDetail.get("amount");
                            if (amtObj instanceof Number) {
                                int guidelineAmt = ((Number) amtObj).intValue();
                                if (guidelineAmt > 0) {
                                    int spent = currentMonthCats.getOrDefault(catName, 0);
                                    if (spent >= guidelineAmt) {
                                        String catKey = "CAT_ALERT_" + monthYear + "_" + catName;
                                        String catTitle = catName + " Spending Notice";
                                        String catMsg = "You have spent ₹" + String.format("%,d", spent) + " on " + catName +
                                                        ", reaching 100% of your suggested guideline (₹" + String.format("%,d", guidelineAmt) + ").";
                                        expenseDAO.saveNotification(catKey, catTitle, catMsg, "CATEGORY_ALERT");
                                    } else if (spent >= (int) (guidelineAmt * 0.8)) {
                                        String catKey = "CAT_PACE_" + monthYear + "_" + catName;
                                        String catTitle = catName + " Pacing Alert";
                                        String catMsg = "You have spent ₹" + String.format("%,d", spent) + " on " + catName +
                                                        " (80% of suggested guideline ₹" + String.format("%,d", guidelineAmt) + ").";
                                        expenseDAO.saveNotification(catKey, catTitle, catMsg, "CATEGORY_PACE");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Non-blocking for notification generation
        }
    }

    // =========================================================================
    // Monthly Budget Notifications & Alerts Engine (Idempotent by UNIQUE keys)
    // =========================================================================
    public void checkAndGenerateMonthlyBudgetNotifications(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(monthYear);
        } catch (Exception e) {
            ym = YearMonth.now();
            monthYear = ym.toString();
        }

        MonthlyBudgetSummary summary = getCurrentMonthlyBudget(monthYear);
        double budget = summary.getBudget();
        double totalSpent = summary.getTotalSpent();
        double remaining = summary.getRemaining();
        double usagePct = summary.getUsagePercentage();
        String monthName = summary.getMonthName();

        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        // Condition 1: No monthly budget set
        String unsetKey = "MONTHLY_BUDGET_UNSET_" + monthYear;
        if (budget <= 0) {
            String title = "No Monthly Budget Set: " + monthName;
            String message = (totalSpent > 0)
                ? String.format("No monthly budget set for %s (₹%,d spent so far). Set a monthly budget to track your spending and receive alerts.", monthName, (int) totalSpent)
                : String.format("No monthly budget set for %s yet. Set a monthly budget to track your spending and receive alerts.", monthName);
            expenseDAO.saveNotification(unsetKey, title, message, "MONTHLY_BUDGET_INFO");
        } else {
            expenseDAO.deleteNotificationByKey(unsetKey);
        }

        if (budget > 0) {
            // Condition 2: Spending reaches 50%
            String k50 = "MONTHLY_BUDGET_50_" + monthYear;
            if (usagePct >= 50.0) {
                String title = "Monthly Budget: 50% Reached";
                String pctStr = (Math.abs(usagePct - Math.round(usagePct)) < 0.05)
                    ? String.format("%d%%", (int) Math.round(usagePct))
                    : String.format("%.1f%%", usagePct);
                String message = String.format("You have used %s of your monthly budget (₹%,d of ₹%,d spent). You have ₹%,d remaining.",
                    pctStr, (int) totalSpent, (int) budget, (int) remaining);
                expenseDAO.saveNotification(k50, title, message, "MONTHLY_BUDGET_INFO");
            } else {
                expenseDAO.deleteNotificationByKey(k50);
            }

            // Condition 3: Spending reaches 80% (Warning)
            String k80 = "MONTHLY_BUDGET_80_" + monthYear;
            if (usagePct >= 80.0) {
                String title = "Monthly Budget Warning: 80% Reached";
                String pctStr = (Math.abs(usagePct - Math.round(usagePct)) < 0.05)
                    ? String.format("%d%%", (int) Math.round(usagePct))
                    : String.format("%.1f%%", usagePct);
                String message = String.format("You have used %s of your monthly budget. Consider reducing spending. (₹%,d of ₹%,d spent, ₹%,d remaining).",
                    pctStr, (int) totalSpent, (int) budget, (int) remaining);
                expenseDAO.saveNotification(k80, title, message, "MONTHLY_BUDGET_WARNING");
            } else {
                expenseDAO.deleteNotificationByKey(k80);
            }

            // Condition 4: Spending reaches 100%
            String k100 = "MONTHLY_BUDGET_100_" + monthYear;
            if (usagePct >= 100.0) {
                String title = "Monthly Budget Alert: Limit Reached";
                String message = String.format("You have reached your monthly budget limit (₹%,d spent of ₹%,d budget).",
                    (int) totalSpent, (int) budget);
                expenseDAO.saveNotification(k100, title, message, "MONTHLY_BUDGET_ALERT");
            } else {
                expenseDAO.deleteNotificationByKey(k100);
            }

            // Condition 5: Budget exceeded (Alert)
            String kExceeded = "MONTHLY_BUDGET_EXCEEDED_" + monthYear;
            if (totalSpent > budget) {
                int overAmt = (int) (totalSpent - budget);
                String title = "Monthly Budget Alert: Budget Exceeded";
                String message = String.format("You have exceeded your monthly budget by ₹%,d (₹%,d spent of ₹%,d budget).",
                    overAmt, (int) totalSpent, (int) budget);
                expenseDAO.saveNotification(kExceeded, title, message, "MONTHLY_BUDGET_ALERT");
            } else {
                expenseDAO.deleteNotificationByKey(kExceeded);
            }

            // Condition 6: High spending / pacing warning when appropriate
            String kPace = "MONTHLY_BUDGET_PACE_" + monthYear;
            double expectedPct = ((double) dayOfMonth / daysInMonth) * 100.0;
            if (dayOfMonth <= 20 && usagePct >= 40.0 && usagePct >= (expectedPct + 20.0) && totalSpent <= budget) {
                String title = "Monthly Spending Pacing Notice";
                String message = String.format("Pacing Warning: On Day %d of %s (%.0f%% through the month), you have already used %.1f%% of your budget (₹%,d of ₹%,d). Consider slowing down your spending.",
                    dayOfMonth, monthName, expectedPct, usagePct, (int) totalSpent, (int) budget);
                expenseDAO.saveNotification(kPace, title, message, "MONTHLY_BUDGET_WARNING");
            } else {
                expenseDAO.deleteNotificationByKey(kPace);
            }
        } else {
            // Budget <= 0: clean up budget-dependent threshold notifications
            expenseDAO.deleteNotificationByKey("MONTHLY_BUDGET_50_" + monthYear);
            expenseDAO.deleteNotificationByKey("MONTHLY_BUDGET_80_" + monthYear);
            expenseDAO.deleteNotificationByKey("MONTHLY_BUDGET_100_" + monthYear);
            expenseDAO.deleteNotificationByKey("MONTHLY_BUDGET_EXCEEDED_" + monthYear);
            expenseDAO.deleteNotificationByKey("MONTHLY_BUDGET_PACE_" + monthYear);
        }
    }

    public void syncNotificationsForDate(String dateStr) {
        String monthYear = YearMonth.now().toString();
        if (dateStr != null && dateStr.length() >= 7) {
            try {
                monthYear = dateStr.substring(0, 7);
            } catch (Exception ignored) {}
        }
        checkAndGenerateMonthlyBudgetNotifications(monthYear);
        checkAndGenerateWeeklyPacing(monthYear);
        String currentMonth = YearMonth.now().toString();
        if (!currentMonth.equals(monthYear)) {
            checkAndGenerateMonthlyBudgetNotifications(currentMonth);
            checkAndGenerateWeeklyPacing(currentMonth);
        }
    }

    public List<NotificationItem> getMonthlyNotifications(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = YearMonth.now().toString();
        }
        return expenseDAO.getMonthlyNotifications(monthYear);
    }

    public List<NotificationItem> getInAppNotifications() {
        return expenseDAO.getNotifications();
    }

    public boolean markNotificationRead(int id) {
        return expenseDAO.markNotificationAsRead(id);
    }

    public boolean markAllNotificationsRead() {
        return expenseDAO.markAllNotificationsAsRead();
    }
}

