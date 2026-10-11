public class WeeklyBudgetSummary {
    private String week;            // e.g. "2026-W40"
    private String weekLabel;       // e.g. "Week 40, 2026"
    private String startDate;       // e.g. "2026-09-28"
    private String endDate;         // e.g. "2026-10-04"
    private String dateRange;       // e.g. "28 Sep - 04 Oct, 2026"
    private double budget;          // e.g. 5000.0
    private double totalSpent;      // e.g. 2000.0
    private double remaining;       // e.g. 3000.0
    private double usagePercentage; // e.g. 40.0
    private String status;          // "Within budget", "Near budget limit", "Budget exceeded", "No budget set"

    public WeeklyBudgetSummary() {}

    public WeeklyBudgetSummary(String week, String weekLabel, String startDate, String endDate, String dateRange,
                               double budget, double totalSpent, double remaining, double usagePercentage, String status) {
        this.week = week;
        this.weekLabel = weekLabel;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateRange = dateRange;
        this.budget = budget;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.usagePercentage = usagePercentage;
        this.status = status;
    }

    public String getWeek() { return week; }
    public void setWeek(String week) { this.week = week; }

    public String getWeekLabel() { return weekLabel; }
    public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }

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
