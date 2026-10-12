import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AiParserService verifying multilingual semantic NLP parsing
 * for English, Telugu, and Tanglish without requiring external network or database.
 */
class AiParserServiceTest {

    private AiParserService parserService;
    private String todayDate;
    private String yesterdayDate;
    private String dayBeforeYesterdayDate;

    @BeforeEach
    void setUp() {
        parserService = AiParserService.getInstance();
        LocalDate now = LocalDate.now();
        todayDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        yesterdayDate = now.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        dayBeforeYesterdayDate = now.minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Test
    @DisplayName("Should return UNKNOWN intent for null or blank input")
    void testNullAndEmptyInput() {
        AiParserService.AiParseResult nullResult = parserService.parse(null);
        assertNotNull(nullResult);
        assertEquals("UNKNOWN", nullResult.intent);
        assertEquals("", nullResult.rawInput);

        AiParserService.AiParseResult emptyResult = parserService.parse("   ");
        assertNotNull(emptyResult);
        assertEquals("UNKNOWN", emptyResult.intent);
        assertEquals("", emptyResult.rawInput);
    }

    @Test
    @DisplayName("Should parse standard English expense for utilities today")
    void testEnglishExpenseParsingUtilities() {
        String input = "Paid 1200 for electricity bill today";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(1200, result.amount);
        assertEquals("Utilities", result.category);
        assertEquals(todayDate, result.date);
        assertTrue(result.confidence > 0.8);
    }

    @Test
    @DisplayName("Should parse English expense with comma-formatted amount and shopping category")
    void testEnglishExpenseShoppingYesterday() {
        String input = "Spent 3,500 on shopping yesterday";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(3500, result.amount);
        assertEquals("Shopping", result.category);
        assertEquals(yesterdayDate, result.date);
    }

    @Test
    @DisplayName("Should parse Telugu expense for food/vegetables today")
    void testTeluguExpenseVegetablesToday() {
        String input = "ఈరోజు కూరగాయలకు 500 ఖర్చు చేశాను";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(500, result.amount);
        assertEquals("Food", result.category);
        assertEquals(todayDate, result.date);
        assertTrue(result.confidence >= 0.9);
    }

    @Test
    @DisplayName("Should parse Telugu expense for rent with yesterday date")
    void testTeluguExpenseRentYesterday() {
        String input = "నిన్న రూమ్ రెంట్ 12000 ఖర్చు చేశాను";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(12000, result.amount);
        assertEquals("Rent", result.category);
        assertEquals(yesterdayDate, result.date);
    }

    @Test
    @DisplayName("Should parse Tanglish expense (Telugu written in Roman script) for food")
    void testTanglishExpenseLunch() {
        String input = "nanna lunch kosam 300 rupees spend chesanu";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(300, result.amount);
        assertEquals("Food", result.category);
        assertEquals(todayDate, result.date);
    }

    @Test
    @DisplayName("Should parse Tanglish expense for transportation petrol")
    void testTanglishExpensePetrolTransportation() {
        String input = "petrol kosam 500 rs spent chesanu";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(500, result.amount);
        assertEquals("Transportation", result.category);
        assertEquals(todayDate, result.date);
    }

    @Test
    @DisplayName("Should resolve 'k' multiplier to thousands (15k -> 15000)")
    void testAmountExtractionWithKMultiplier() {
        String input = "Spent 15k on shopping";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(15000, result.amount);
        assertEquals("Shopping", result.category);
    }

    @Test
    @DisplayName("Should resolve Telugu 'వేలు' multiplier to thousands (50 వేలు -> 50000)")
    void testAmountExtractionWithTeluguThousandsMultiplier() {
        String input = "కొత్త బట్టలు కోసం 50 వేలు ఖర్చు";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("EXPENSE", result.intent);
        assertEquals(50000, result.amount);
        assertEquals("Shopping", result.category);
    }

    @Test
    @DisplayName("Should resolve 'lakh' and 'లక్ష' multipliers to 100,000")
    void testAmountExtractionWithLakhMultiplier() {
        String inputEnglish = "Car down payment 2 lakh spent";
        AiParserService.AiParseResult resultEnglish = parserService.parse(inputEnglish);
        assertEquals(200000, resultEnglish.amount);

        String inputTelugu = "ఆఫీస్ కోసం 3 లక్ష ఖర్చు";
        AiParserService.AiParseResult resultTelugu = parserService.parse(inputTelugu);
        assertEquals(300000, resultTelugu.amount);
    }

    @Test
    @DisplayName("Should correctly parse 'day before yesterday' and Telugu 'మొన్న' relative dates")
    void testRelativeDateDayBeforeYesterday() {
        String inputEnglish = "Spent 1200 on fuel day before yesterday";
        AiParserService.AiParseResult resultEnglish = parserService.parse(inputEnglish);
        assertEquals(dayBeforeYesterdayDate, resultEnglish.date);
        assertEquals("Transportation", resultEnglish.category);

        String inputTelugu = "మొన్న సినిమా కి 600 ఖర్చు చేశాను";
        AiParserService.AiParseResult resultTelugu = parserService.parse(inputTelugu);
        assertEquals(dayBeforeYesterdayDate, resultTelugu.date);
        assertEquals("Entertainment", resultTelugu.category);
    }

    @Test
    @DisplayName("Should classify BUDGET intent for salary allocation in English")
    void testBudgetIntentClassificationEnglish() {
        String input = "My salary is 60000 plan budget for food and rent";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("BUDGET", result.intent);
        assertEquals(60000, result.salary);
        assertTrue(result.categories.contains("Food"), "Expected categories to include Food");
        assertTrue(result.categories.contains("Rent"), "Expected categories to include Rent");
    }

    @Test
    @DisplayName("Should classify BUDGET intent for salary allocation in Telugu with 'వేలు'")
    void testBudgetIntentClassificationTelugu() {
        String input = "నా జీతం 50 వేలు బడ్జెట్ కావాలి ఫుడ్ రెంట్ సేవింగ్స్";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("BUDGET", result.intent);
        assertEquals(50000, result.salary);
        assertTrue(result.categories.contains("Food"), "Expected categories to include Food");
        assertTrue(result.categories.contains("Rent"), "Expected categories to include Rent");
        assertTrue(result.categories.contains("Savings"), "Expected categories to include Savings");
    }

    @Test
    @DisplayName("Should provide default budget categories if none are explicitly mentioned")
    void testBudgetIntentDefaultCategories() {
        String input = "Salary is 45000 calculate budget";
        AiParserService.AiParseResult result = parserService.parse(input);

        assertNotNull(result);
        assertEquals("BUDGET", result.intent);
        assertEquals(45000, result.salary);
        assertFalse(result.categories.isEmpty());
        assertTrue(result.categories.contains("Rent"));
        assertTrue(result.categories.contains("Food"));
        assertTrue(result.categories.contains("Savings"));
        assertTrue(result.categories.contains("Others"));
    }
}
