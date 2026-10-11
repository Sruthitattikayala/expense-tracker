public class BudgetPlan {
    private String monthYear;
    private int salary;
    private String allocationsJson;
    private String updatedAt;

    public BudgetPlan() {}

    public BudgetPlan(String monthYear, int salary, String allocationsJson, String updatedAt) {
        this.monthYear = monthYear;
        this.salary = salary;
        this.allocationsJson = allocationsJson;
        this.updatedAt = updatedAt;
    }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public int getSalary() { return salary; }
    public void setSalary(int salary) { this.salary = salary; }

    public String getAllocationsJson() { return allocationsJson; }
    public void setAllocationsJson(String allocationsJson) { this.allocationsJson = allocationsJson; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
