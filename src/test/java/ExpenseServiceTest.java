import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService business logic, testing budget algorithms,
 * Month-over-Month calculations, ISO week boundaries, and alert thresholds
 * using Mockito to isolate the service layer from live MySQL connections.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseDAO expenseDAO;

    @InjectMocks
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() throws Exception {
        // Explicit reflection injection ensures mock is bound cleanly to the private final field
        Field field = ExpenseService.class.getDeclaredField("expenseDAO");
        field.setAccessible(true);
        field.set(expenseService, expenseDAO);
    }

    // =========================================================================
    // 1. 50/30/20 Budget Allocation Calculations
    // =========================================================================

    @Test
    @DisplayName("Should proportionally calculate suggested budget guidelines for custom categories")
    void testCalculateSuggestedBudget_WithCustomCategories() {
        int salary = 50000;
        List<String> categories = Arrays.asList("Rent", "Food", "Savings", "Utilities");

        Map<String, Object> result = expenseService.calculateSuggestedBudget(salary, categories);

        assertNotNull(result);
        assertEquals(salary, result.get("salary"));
        assertTrue((Boolean) result.get("isGuidelineOnly"));
        assertNotNull(result.get("disclaimer"));

        int totalAllocated = (Integer) result.get("totalAllocated");
        int unallocated = (Integer) result.get("unallocated");
        assertEquals(salary, totalAllocated + unallocated, "Total allocated plus unallocated must equal salary");

        @SuppressWarnings("unchecked")
        Map<String, Object> guidelines = (Map<String, Object>) result.get("guidelines");
        assertNotNull(guidelines);
        assertEquals(4, guidelines.size());

        assertTrue(guidelines.containsKey("Rent"));
        assertTrue(guidelines.containsKey("Food"));
        assertTrue(guidelines.containsKey("Savings"));
        assertTrue(guidelines.containsKey("Utilities"));

        // Rent has highest weight (30), so its allocated amount must be greater than Food (20)
        @SuppressWarnings("unchecked")
        Map<String, Object> rentDetail = (Map<String, Object>) guidelines.get("Rent");
        @SuppressWarnings("unchecked")
        Map<String, Object> foodDetail = (Map<String, Object>) guidelines.get("Food");

        int rentAmount = (Integer) rentDetail.get("amount");
        int foodAmount = (Integer) foodDetail.get("amount");
        assertTrue(rentAmount > foodAmount, "Rent allocation should exceed Food allocation based on weights");
    }

    @Test
    @DisplayName("Should use default baseline categories when category list is null or empty")
    void testCalculateSuggestedBudget_DefaultCategories() {
        int salary = 60000;

        Map<String, Object> resultFromNull = expenseService.calculateSuggestedBudget(salary, null);
        assertNotNull(resultFromNull);

        @SuppressWarnings("unchecked")
        Map<String, Object> guidelines = (Map<String, Object>) resultFromNull.get("guidelines");
        assertNotNull(guidelines);
        assertTrue(guidelines.containsKey("Rent"));
        assertTrue(guidelines.containsKey("Food"));
        assertTrue(guidelines.containsKey("Savings"));
        assertTrue(guidelines.containsKey("Others"));

        int totalAllocated = (Integer) resultFromNull.get("totalAllocated");
        int unallocated = (Integer) resultFromNull.get("unallocated");
        assertEquals(salary, totalAllocated + unallocated);
    }

    // =========================================================================
    // 2. Month-over-Month Spending Comparisons
    // =========================================================================

    @Test
    @DisplayName("Should compute positive difference and percentage change when spending increases")
    void testMonthlyComparison_SpendingIncreased() {
        String curMonth = "2026-10";
        String prevMonth = "2026-09";

        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(25000.0);
        when(expenseDAO.getMonthlyExpensesTotal("2026-09-01", "2026-09-30")).thenReturn(20000.0);
        when(expenseDAO.getMonthlyCategoryBreakdown(anyString(), anyString())).thenReturn(Collections.emptyMap());

        Map<String, Object> comp = expenseService.getMonthlyComparison(curMonth, prevMonth);

        assertEquals(25000.0, comp.get("currentTotal"));
        assertEquals(20000.0, comp.get("previousTotal"));
        assertEquals(5000.0, comp.get("difference"));
        assertEquals(25.0, comp.get("percentageChange"), "Expected +25.0% increase");
    }

    @Test
    @DisplayName("Should compute negative difference and percentage change when spending decreases")
    void testMonthlyComparison_SpendingDecreased() {
        String curMonth = "2026-10";
        String prevMonth = "2026-09";

        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(15000.0);
        when(expenseDAO.getMonthlyExpensesTotal("2026-09-01", "2026-09-30")).thenReturn(20000.0);
        when(expenseDAO.getMonthlyCategoryBreakdown(anyString(), anyString())).thenReturn(Collections.emptyMap());

        Map<String, Object> comp = expenseService.getMonthlyComparison(curMonth, prevMonth);

        assertEquals(15000.0, comp.get("currentTotal"));
        assertEquals(20000.0, comp.get("previousTotal"));
        assertEquals(-5000.0, comp.get("difference"));
        assertEquals(-25.0, comp.get("percentageChange"), "Expected -25.0% decrease");
    }

    @Test
    @DisplayName("Should compute zero difference and 0.0% change when spending is equal")
    void testMonthlyComparison_SpendingEqual() {
        String curMonth = "2026-10";
        String prevMonth = "2026-09";

        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(20000.0);
        when(expenseDAO.getMonthlyExpensesTotal("2026-09-01", "2026-09-30")).thenReturn(20000.0);
        when(expenseDAO.getMonthlyCategoryBreakdown(anyString(), anyString())).thenReturn(Collections.emptyMap());

        Map<String, Object> comp = expenseService.getMonthlyComparison(curMonth, prevMonth);

        assertEquals(0.0, comp.get("difference"));
        assertEquals(0.0, comp.get("percentageChange"));
    }

    @Test
    @DisplayName("Should report 100.0% change when previous spending was zero and current is positive")
    void testMonthlyComparison_PreviousZeroCurrentPositive() {
        String curMonth = "2026-10";
        String prevMonth = "2026-09";

        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(10000.0);
        when(expenseDAO.getMonthlyExpensesTotal("2026-09-01", "2026-09-30")).thenReturn(0.0);
        when(expenseDAO.getMonthlyCategoryBreakdown(anyString(), anyString())).thenReturn(Collections.emptyMap());

        Map<String, Object> comp = expenseService.getMonthlyComparison(curMonth, prevMonth);

        assertEquals(10000.0, comp.get("difference"));
        assertEquals(100.0, comp.get("percentageChange"));
    }

    // =========================================================================
    // 3. Monthly and Weekly Budget Calculations & ISO Week Boundaries
    // =========================================================================

    @Test
    @DisplayName("Should calculate monthly budget within budget correctly")
    void testMonthlyBudget_WithinBudget() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 30000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(15000.0);

        MonthlyBudgetSummary summary = expenseService.getCurrentMonthlyBudget(month);

        assertNotNull(summary);
        assertEquals(30000.0, summary.getBudget());
        assertEquals(15000.0, summary.getTotalSpent());
        assertEquals(15000.0, summary.getRemaining());
        assertEquals(50.0, summary.getUsagePercentage());
        assertEquals("Within budget", summary.getStatus());
    }

    @Test
    @DisplayName("Should report 'Near budget limit' when spending reaches 80% or more")
    void testMonthlyBudget_NearLimit() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 20000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(17000.0);

        MonthlyBudgetSummary summary = expenseService.getCurrentMonthlyBudget(month);

        assertEquals(20000.0, summary.getBudget());
        assertEquals(17000.0, summary.getTotalSpent());
        assertEquals(3000.0, summary.getRemaining());
        assertEquals(85.0, summary.getUsagePercentage());
        assertEquals("Near budget limit", summary.getStatus());
    }

    @Test
    @DisplayName("Should report 'Budget exceeded' when spending surpasses allocated budget")
    void testMonthlyBudget_Exceeded() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 20000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(22000.0);

        MonthlyBudgetSummary summary = expenseService.getCurrentMonthlyBudget(month);

        assertEquals(20000.0, summary.getBudget());
        assertEquals(22000.0, summary.getTotalSpent());
        assertEquals(-2000.0, summary.getRemaining());
        assertEquals(110.0, summary.getUsagePercentage());
        assertEquals("Budget exceeded", summary.getStatus());
    }

    @Test
    @DisplayName("Should correctly calculate ISO week boundaries and summary for specified week")
    void testWeeklyBudget_IsoWeekBoundaries() {
        // Week 41 of 2026 is Monday 05 Oct 2026 to Sunday 11 Oct 2026
        String weekKey = "2026-W41";

        when(expenseDAO.getWeeklyBudget(weekKey)).thenReturn(6000.0);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-05", "2026-10-11")).thenReturn(3000.0);

        WeeklyBudgetSummary summary = expenseService.getCurrentWeeklyBudget(weekKey);

        assertNotNull(summary);
        assertEquals(weekKey, summary.getWeek());
        assertEquals("2026-10-05", summary.getStartDate(), "Start date must be Monday of ISO Week 41");
        assertEquals("2026-10-11", summary.getEndDate(), "End date must be Sunday of ISO Week 41");
        assertTrue(summary.getDateRange().contains("05 Oct") && summary.getDateRange().contains("11 Oct, 2026"));
        assertEquals(6000.0, summary.getBudget());
        assertEquals(3000.0, summary.getTotalSpent());
        assertEquals(3000.0, summary.getRemaining());
        assertEquals(50.0, summary.getUsagePercentage());
        assertEquals("Within budget", summary.getStatus());
    }

    // =========================================================================
    // 4. Behavior When No Budget Is Set
    // =========================================================================

    @Test
    @DisplayName("Should return 'No budget set' with 0 budget and remaining when monthly budget is unconfigured")
    void testMonthlyBudget_WhenNoBudgetSet() {
        String month = "2026-11";

        when(expenseDAO.getBudget(month)).thenReturn(null);
        when(expenseDAO.getMonthlyExpensesTotal("2026-11-01", "2026-11-30")).thenReturn(0.0);

        MonthlyBudgetSummary summary = expenseService.getCurrentMonthlyBudget(month);

        assertNotNull(summary);
        assertEquals(0.0, summary.getBudget());
        assertEquals(0.0, summary.getTotalSpent());
        assertEquals(0.0, summary.getRemaining());
        assertEquals(0.0, summary.getUsagePercentage());
        assertEquals("No budget set", summary.getStatus());
    }

    @Test
    @DisplayName("Should return 'No budget set' when weekly budget is unconfigured")
    void testWeeklyBudget_WhenNoBudgetSet() {
        String weekKey = "2026-W45";

        when(expenseDAO.getWeeklyBudget(weekKey)).thenReturn(null);
        when(expenseDAO.getMonthlyExpensesTotal(anyString(), anyString())).thenReturn(0.0);

        WeeklyBudgetSummary summary = expenseService.getCurrentWeeklyBudget(weekKey);

        assertNotNull(summary);
        assertEquals(0.0, summary.getBudget());
        assertEquals(0.0, summary.getRemaining());
        assertEquals(0.0, summary.getUsagePercentage());
        assertEquals("No budget set", summary.getStatus());
    }

    // =========================================================================
    // 5. Notification Threshold Triggers
    // =========================================================================

    @Test
    @DisplayName("Should generate UNSET notification when no monthly budget is configured")
    void testMonthlyNotifications_UnsetNotice() {
        String month = "2026-10";

        when(expenseDAO.getBudget(month)).thenReturn(null);
        when(expenseDAO.getMonthlyExpensesTotal(anyString(), anyString())).thenReturn(0.0);

        expenseService.checkAndGenerateMonthlyBudgetNotifications(month);

        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_UNSET_2026-10"),
            contains("No Monthly Budget Set"),
            anyString(),
            eq("MONTHLY_BUDGET_INFO")
        );
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_50_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_80_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_100_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_EXCEEDED_2026-10");
    }

    @Test
    @DisplayName("Should trigger 50% threshold notification when spending reaches half of budget")
    void testMonthlyNotifications_50PercentThreshold() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 10000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(5000.0);

        expenseService.checkAndGenerateMonthlyBudgetNotifications(month);

        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_50_2026-10"),
            eq("Monthly Budget: 50% Reached"),
            contains("You have used 50% of your monthly budget"),
            eq("MONTHLY_BUDGET_INFO")
        );
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_UNSET_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_80_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_100_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_EXCEEDED_2026-10");
    }

    @Test
    @DisplayName("Should trigger 80% warning notification when spending reaches 80% of budget")
    void testMonthlyNotifications_80PercentWarning() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 10000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(8200.0);

        expenseService.checkAndGenerateMonthlyBudgetNotifications(month);

        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_50_2026-10"),
            eq("Monthly Budget: 50% Reached"),
            anyString(),
            eq("MONTHLY_BUDGET_INFO")
        );
        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_80_2026-10"),
            eq("Monthly Budget Warning: 80% Reached"),
            contains("Consider reducing spending"),
            eq("MONTHLY_BUDGET_WARNING")
        );
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_100_2026-10");
        verify(expenseDAO).deleteNotificationByKey("MONTHLY_BUDGET_EXCEEDED_2026-10");
    }

    @Test
    @DisplayName("Should trigger 100% alert and EXCEEDED alert when spending surpasses budget")
    void testMonthlyNotifications_100PercentAndExceededAlert() {
        String month = "2026-10";
        BudgetPlan plan = new BudgetPlan(month, 10000, "{}", "2026-10-01");

        when(expenseDAO.getBudget(month)).thenReturn(plan);
        when(expenseDAO.getMonthlyExpensesTotal("2026-10-01", "2026-10-31")).thenReturn(12000.0);

        expenseService.checkAndGenerateMonthlyBudgetNotifications(month);

        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_100_2026-10"),
            eq("Monthly Budget Alert: Limit Reached"),
            contains("You have reached your monthly budget limit"),
            eq("MONTHLY_BUDGET_ALERT")
        );
        verify(expenseDAO).saveNotification(
            eq("MONTHLY_BUDGET_EXCEEDED_2026-10"),
            eq("Monthly Budget Alert: Budget Exceeded"),
            contains("You have exceeded your monthly budget by ₹2,000"),
            eq("MONTHLY_BUDGET_ALERT")
        );
    }
}
