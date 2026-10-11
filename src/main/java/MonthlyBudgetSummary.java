public class MonthlyBudgetSummary {
    private String month;            // e.g. "2026-10"
    private String monthName;        // e.g. "October 2026"
    private double budget;           // e.g. 20000.0
    private double totalSpent;       // e.g. 8500.0
    private double remaining;        // e.g. 11500.0
    private double usagePercentage;  // e.g. 42.5
    private String status;           // "Within budget", "Near budget limit", "Budget exceeded", "No budget set"

    public MonthlyBudgetSummary() {}

    public MonthlyBudgetSummary(String month, String monthName, double budget, double totalSpent, double remaining, double usagePercentage, String status) {
        this.month = month;
        this.monthName = monthName;
        this.budget = budget;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.usagePercentage = usagePercentage;
        this.status = status;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public double getRemaining() { return remaining; }
    public void setRemaining(double remaining) { this.remaining = remaining; }

    public double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
