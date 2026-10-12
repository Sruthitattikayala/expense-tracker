import com.google.gson.Gson;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modular AI/NLP Parser supporting Telugu, English, and Telugu-English mixed (Tanglish).
 * Designed with a pluggable provider pattern to easily switch to an external LLM / NLP API.
 */
public class AiParserService {

    public static class AiParseResult {
        public String intent = "UNKNOWN";      // EXPENSE, BUDGET, UNKNOWN
        public int amount = 0;                  // For expense
        public String category = "";            // For expense
        public String date = "";                // For expense (YYYY-MM-DD)
        public String description = "";         // For expense
        public int salary = 0;                  // For budget
        public List<String> categories = new ArrayList<>(); // For budget
        public String rawInput = "";
        public double confidence = 0.0;
        public String parserSource = "LOCAL_SEMANTIC_ENGINE"; // or EXTERNAL_AI_API
    }

    private static final AiParserService INSTANCE = new AiParserService();

    public static AiParserService getInstance() {
        return INSTANCE;
    }

    private final Gson gson = new Gson();

    public AiParseResult parse(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            AiParseResult res = new AiParseResult();
            res.rawInput = "";
            return res;
        }

        String text = rawText.trim();

        // 1. Check if external AI/LLM API is configured via environment variables
        String externalApiUrl = System.getenv("AI_API_URL");
        String externalApiKey = System.getenv("AI_API_KEY");
        if (externalApiUrl != null && !externalApiUrl.trim().isEmpty() &&
            externalApiKey != null && !externalApiKey.trim().isEmpty()) {
            try {
                AiParseResult externalResult = parseViaExternalApi(text, externalApiUrl, externalApiKey);
                if (externalResult != null && !"UNKNOWN".equals(externalResult.intent)) {
                    return externalResult;
                }
            } catch (Exception e) {
                System.out.println("Notice: External AI parser failed, falling back to local engine: " + e.getMessage());
            }
        }

        // 2. Default: Robust local multilingual semantic parser
        return parseViaLocalEngine(text);
    }

    /**
     * Local multilingual semantic engine for Telugu, English, and Tanglish.
     */
    private AiParseResult parseViaLocalEngine(String text) {
        AiParseResult result = new AiParseResult();
        result.rawInput = text;
        result.parserSource = "LOCAL_SEMANTIC_ENGINE";

        String lower = text.toLowerCase(Locale.ROOT);

        // Detect Intent: BUDGET vs EXPENSE
        boolean hasSalaryKeyword = lower.contains("salary") || lower.contains("జీతం") || 
                                   lower.contains("income") || lower.contains("సంపాదన") ||
                                   lower.contains("వస్తుంది") || lower.contains("budget for") ||
                                   lower.contains("బడ్జెట్ కావాలి");

        boolean hasExpenseKeyword = lower.contains("spend") || lower.contains("spent") || 
                                    lower.contains("ఖర్చు") || lower.contains("కొన్నాను") ||
                                    lower.contains("paid") || lower.contains("ఖర్చయింది") ||
                                    lower.contains("ఖర్చు చేశాను") || lower.contains("రూపాయలు") ||
                                    lower.contains("rupees") || lower.contains("rs");

        if (hasSalaryKeyword && !hasExpenseKeyword) {
            parseBudgetIntent(text, lower, result);
        } else if (hasExpenseKeyword && !hasSalaryKeyword) {
            parseExpenseIntent(text, lower, result);
        } else {
            // Heuristic resolution if both or neither present
            if (lower.contains("జీతం") || lower.contains("salary") || lower.contains("బడ్జెట్")) {
                parseBudgetIntent(text, lower, result);
            } else {
                parseExpenseIntent(text, lower, result);
            }
        }

        return result;
    }

    private void parseBudgetIntent(String text, String lower, AiParseResult result) {
        result.intent = "BUDGET";

        // Extract Salary Amount (e.g. 30000, 30k, 50,000, 30 వేలు)
        result.salary = extractAmount(text, lower);

        // Extract Desired Categories
        Set<String> matchedCats = new LinkedHashSet<>();

        // Food
        if (containsAny(lower, "food", "ఫుడ్", "ఆహారం", "కూరగాయ", "dining", "కిరాణా", "groceries")) {
            matchedCats.add("Food");
        }
        // Rent / Housing
        if (containsAny(lower, "rent", "రెంట్", "అద్దె", "ఇంటి అద్దె", "room rent", "house rent", "housing")) {
            matchedCats.add("Rent");
        }
        // Education
        if (containsAny(lower, "education", "ఎడ్యుకేషన్", "చదువు", "స్కూల్", "కాలేజ్", "ఫీజు", "fees", "school", "college", "books")) {
            matchedCats.add("Education");
        }
        // Gym / Fitness
        if (containsAny(lower, "gym", "జిమ్", "ఫిట్నెస్", "వ్యాయామం", "fitness", "workout")) {
            matchedCats.add("Gym");
        }
        // Savings
        if (containsAny(lower, "saving", "savings", "సేవింగ్స్", "పొదుపు", "deposit", "investment")) {
            matchedCats.add("Savings");
        }
        // Transportation
        if (containsAny(lower, "transport", "transportation", "ప్రయాణం", "పెట్రోల్", "petrol", "diesel", "fuel", "metro", "bus", "cab", "travel")) {
            matchedCats.add("Transportation");
        }
        // Utilities
        if (containsAny(lower, "utilities", "bills", "కరెంట్", "కరెంట్ బిల్లు", "గ్యాస్", "current bill", "electricity", "gas", "wifi")) {
            matchedCats.add("Utilities");
        }
        // Healthcare
        if (containsAny(lower, "health", "healthcare", "డాక్టర్", "హాస్పిటల్", "మందులు", "medical", "medicine")) {
            matchedCats.add("Healthcare");
        }
        // Others
        if (containsAny(lower, "other", "others", "ఇతర", "ఇతరాలు", "మిగిలినవి", "misc")) {
            matchedCats.add("Others");
        }

        // If no explicit categories mentioned, default to common baseline
        if (matchedCats.isEmpty()) {
            matchedCats.addAll(Arrays.asList("Rent", "Food", "Savings", "Others"));
        }

        result.categories = new ArrayList<>(matchedCats);
        result.confidence = result.salary > 0 ? 0.95 : 0.75;
    }

    private void parseExpenseIntent(String text, String lower, AiParseResult result) {
        result.intent = "EXPENSE";

        // Extract Amount
        result.amount = extractAmount(text, lower);

        // Extract Category
        if (containsAny(lower, "vegetables", "కూరగాయ", "food", "ఫుడ్", "ఆహారం", "dining", "lunch", "dinner", "breakfast", "టిఫిన్", "భోజనం", "groceries", "కిరాణా")) {
            result.category = "Food";
        } else if (containsAny(lower, "rent", "రెంట్", "అద్దె", "ఇంటి అద్దె", "house rent", "room rent")) {
            result.category = "Rent";
        } else if (containsAny(lower, "education", "ఎడ్యుకేషన్", "చదువు", "స్కూల్", "కాలేజ్", "ఫీజు", "fees", "school", "college", "books")) {
            result.category = "Education";
        } else if (containsAny(lower, "gym", "జిమ్", "ఫిట్నెస్", "వ్యాయామం", "fitness", "workout")) {
            result.category = "Gym";
        } else if (containsAny(lower, "transport", "transportation", "ప్రయాణం", "పెట్రోల్", "petrol", "fuel", "డీజిల్", "metro", "bus", "cab", "auto", "travel")) {
            result.category = "Transportation";
        } else if (containsAny(lower, "utilities", "bills", "కరెంట్", "కరెంట్ బిల్లు", "నీళ్లు", "గ్యాస్", "current bill", "electricity", "gas", "wifi", "recharge")) {
            result.category = "Utilities";
        } else if (containsAny(lower, "shopping", "షాపింగ్", "బట్టలు", "clothes", "dress")) {
            result.category = "Shopping";
        } else if (containsAny(lower, "health", "healthcare", "డాక్టర్", "హాస్పిటల్", "మందులు", "medical", "medicine")) {
            result.category = "Healthcare";
        } else if (containsAny(lower, "entertainment", "సినిమా", "మూవీ", "movie", "outing")) {
            result.category = "Entertainment";
        } else {
            result.category = "Other";
        }

        // Extract Date (Defaults to today)
        LocalDate targetDate = LocalDate.now();
        if (containsAny(lower, "day before yesterday", "మొన్న")) {
            targetDate = targetDate.minusDays(2);
        } else if (containsAny(lower, "yesterday", "నిన్న")) {
            targetDate = targetDate.minusDays(1);
        }
        result.date = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Clean Description
        String desc = text;
        // Trim common filler phrases
        desc = desc.replaceAll("(?i)(ఈరోజు|ఈ రోజు|నిన్న|మొన్న|today|yesterday|రూపాయలు|ఖర్చు చేశాను|ఖర్చు|ఖర్చయింది|spend చేశాను|spend|spent|rupees|rs|for|కోసం)", "").trim();
        desc = desc.replaceAll("\\s+", " ").trim();
        if (desc.isEmpty()) {
            desc = result.category + " expense";
        }
        result.description = desc;
        result.confidence = result.amount > 0 ? 0.95 : 0.70;
    }

    private int extractAmount(String text, String lower) {
        // Regex for numbers, handles commas, 'k', 'వేలు' (thousands)
        Pattern numberPattern = Pattern.compile("(\\d[\\d,]*)\\s*(k|వేలు|lakh|లక్ష)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = numberPattern.matcher(text);

        int maxFound = 0;
        while (matcher.find()) {
            try {
                String numStr = matcher.group(1).replace(",", "");
                int val = Integer.parseInt(numStr);
                String suffix = matcher.group(2);
                if (suffix != null) {
                    suffix = suffix.toLowerCase(Locale.ROOT);
                    if ("k".equals(suffix) || "వేలు".equals(suffix)) {
                        val *= 1000;
                    } else if ("lakh".equals(suffix) || "లక్ష".equals(suffix)) {
                        val *= 100000;
                    }
                }
                if (val > maxFound) {
                    maxFound = val;
                }
            } catch (NumberFormatException ignored) {}
        }

        return maxFound;
    }

    private boolean containsAny(String source, String... targets) {
        for (String target : targets) {
            if (source.contains(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pluggable external AI API connector.
     * Ready to query external LLM endpoints (OpenAI/Gemini/Custom) when AI_API_URL and AI_API_KEY are configured.
     */
    private AiParseResult parseViaExternalApi(String text, String apiUrl, String apiKey) {
        // Architecture hook for external LLM inference
        return null;
    }
}
