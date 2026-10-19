import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests deployment-readiness configuration logic:
 * 1. HTTP server port resolution (PORT env/system property with fallback to 8080).
 * 2. Database port resolution (DB_PORT with fallback to 3306).
 * 3. Secure TLS configuration for TiDB Cloud (sslMode=VERIFY_IDENTITY and TLS protocols).
 * 4. Local MySQL compatibility preservation (no enforced TLS verification on localhost).
 * 5. Host, user, and password resolution behavior.
 */
class DeploymentConfigTest {

    @AfterEach
    void cleanupSystemProperties() {
        System.clearProperty("PORT");
        System.clearProperty("DB_PORT");
        System.clearProperty("DB_HOST");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_SSL_MODE");
    }

    // =========================================================================
    // 1. HTTP Server PORT Configuration Tests (Requirements.java)
    // =========================================================================

    @Test
    @DisplayName("Requirements.resolvePort() defaults to 8080 when PORT is not set")
    void testRequirementsPortDefault() {
        System.clearProperty("PORT");
        // Unless environment has PORT, should resolve to default 8080
        if (System.getenv("PORT") == null) {
            assertEquals(8080, Requirements.resolvePort());
        }
        assertEquals(8080, Requirements.DEFAULT_PORT);
    }

    @Test
    @DisplayName("Requirements.resolvePort() respects valid PORT from system property")
    void testRequirementsPortCustom() {
        System.setProperty("PORT", "9090");
        assertEquals(9090, Requirements.resolvePort());

        System.setProperty("PORT", " 3000 ");
        assertEquals(3000, Requirements.resolvePort());
    }

    @Test
    @DisplayName("Requirements.resolvePort() falls back to 8080 for invalid PORT values")
    void testRequirementsPortInvalidFallback() {
        System.setProperty("PORT", "invalid-port");
        if (System.getenv("PORT") == null) {
            assertEquals(8080, Requirements.resolvePort());
        }

        System.setProperty("PORT", "-1");
        if (System.getenv("PORT") == null) {
            assertEquals(8080, Requirements.resolvePort());
        }

        System.setProperty("PORT", "999999");
        if (System.getenv("PORT") == null) {
            assertEquals(8080, Requirements.resolvePort());
        }
    }

    // =========================================================================
    // 2. Database Port & Host Configuration Tests (DBConnection.java)
    // =========================================================================

    @Test
    @DisplayName("DBConnection.resolvePort() defaults to 3306 when DB_PORT is not set")
    void testDbConnectionPortDefault() {
        System.clearProperty("DB_PORT");
        if (System.getenv("DB_PORT") == null) {
            assertEquals(3306, DBConnection.resolvePort());
        }
        assertEquals(3306, DBConnection.DEFAULT_PORT);
    }

    @Test
    @DisplayName("DBConnection.resolvePort() respects valid DB_PORT from system property")
    void testDbConnectionPortCustom() {
        System.setProperty("DB_PORT", "4000");
        assertEquals(4000, DBConnection.resolvePort());

        System.setProperty("DB_PORT", " 3307 ");
        assertEquals(3307, DBConnection.resolvePort());
    }

    @Test
    @DisplayName("DBConnection.resolvePort() falls back to 3306 for invalid DB_PORT values")
    void testDbConnectionPortInvalidFallback() {
        System.setProperty("DB_PORT", "abc");
        if (System.getenv("DB_PORT") == null) {
            assertEquals(3306, DBConnection.resolvePort());
        }

        System.setProperty("DB_PORT", "0");
        if (System.getenv("DB_PORT") == null) {
            assertEquals(3306, DBConnection.resolvePort());
        }

        System.setProperty("DB_PORT", "70000");
        if (System.getenv("DB_PORT") == null) {
            assertEquals(3306, DBConnection.resolvePort());
        }
    }

    @Test
    @DisplayName("DBConnection.resolveHost() and resolveUser() preserve existing behavior")
    void testDbConnectionHostAndUserResolution() {
        String envHost = System.getenv("DB_HOST");
        if (envHost != null && !envHost.trim().isEmpty()) {
            assertEquals(envHost.trim(), DBConnection.resolveHost());
        } else {
            System.clearProperty("DB_HOST");
            assertEquals("localhost", DBConnection.resolveHost());
            System.setProperty("DB_HOST", "tidb-cluster.example.com");
            assertEquals("tidb-cluster.example.com", DBConnection.resolveHost());
        }

        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            assertEquals(envUser.trim(), DBConnection.resolveUser());
        } else {
            System.clearProperty("DB_USER");
            assertEquals("expense_app", DBConnection.resolveUser());
            System.setProperty("DB_USER", "custom_user");
            assertEquals("custom_user", DBConnection.resolveUser());
        }
    }

    // =========================================================================
    // 3. JDBC URL Construction & TLS Security Tests
    // =========================================================================

    @Test
    @DisplayName("Local MySQL URL preserves compatibility with local MySQL without TLS verification")
    void testBuildJdbcUrlLocalMySQLCompatibility() {
        // localhost
        String localUrl = DBConnection.buildJdbcUrl("localhost", 3306);
        assertEquals("jdbc:mysql://localhost:3306/Expense_Tracker", localUrl);
        assertFalse(localUrl.contains("sslMode=VERIFY_IDENTITY"), "Localhost must not enforce VERIFY_IDENTITY");

        // 127.0.0.1
        String loopbackUrl = DBConnection.buildJdbcUrl("127.0.0.1", 3306);
        assertEquals("jdbc:mysql://127.0.0.1:3306/Expense_Tracker", loopbackUrl);

        // host.docker.internal
        String dockerUrl = DBConnection.buildJdbcUrl("host.docker.internal", 3306);
        assertEquals("jdbc:mysql://host.docker.internal:3306/Expense_Tracker", dockerUrl);
    }

    @Test
    @DisplayName("Remote TiDB Cloud URL configures secure TLS with certificate verification")
    void testBuildJdbcUrlTiDbCloudTls() {
        String tidbHost = "gateway01.us-west-2.prod.aws.tidbcloud.com";
        int tidbPort = 4000;

        String tidbUrl = DBConnection.buildJdbcUrl(tidbHost, tidbPort);

        // Must connect to TiDB host and port with correct database
        assertTrue(tidbUrl.startsWith("jdbc:mysql://gateway01.us-west-2.prod.aws.tidbcloud.com:4000/Expense_Tracker"));

        // Must configure secure TLS for TiDB Cloud
        assertTrue(tidbUrl.contains("sslMode=VERIFY_IDENTITY"), "TiDB Cloud URL must require sslMode=VERIFY_IDENTITY");
        assertTrue(tidbUrl.contains("enabledTLSProtocols=TLSv1.2,TLSv1.3"), "TiDB Cloud URL must specify modern TLS protocols");

        // Must NOT disable certificate verification
        assertFalse(tidbUrl.contains("trustServerCertificate=true"), "Certificate verification must NOT be disabled");
        assertFalse(tidbUrl.contains("verifyServerCertificate=false"), "Certificate verification must NOT be disabled");

        // Must NOT hardcode credentials into the URL
        assertFalse(tidbUrl.contains("password="), "Password must not be hardcoded in the JDBC URL");
        assertFalse(tidbUrl.contains("user="), "Username must not be hardcoded in the JDBC URL");
    }

    @Test
    @DisplayName("DB_SSL_MODE property allows explicit override when needed")
    void testDbConnectionSslModeOverride() {
        System.setProperty("DB_SSL_MODE", "VERIFY_CA");
        String url = DBConnection.buildJdbcUrl("localhost", 3306);
        assertTrue(url.contains("sslMode=VERIFY_CA"));
        assertTrue(url.contains("enabledTLSProtocols=TLSv1.2,TLSv1.3"));

        System.setProperty("DB_SSL_MODE", "DISABLED");
        String disabledUrl = DBConnection.buildJdbcUrl("remote.db.com", 3306);
        assertTrue(disabledUrl.contains("sslMode=DISABLED"));
        assertFalse(disabledUrl.contains("enabledTLSProtocols"));
    }

    @Test
    @DisplayName("isLocalHost identifies local hostnames and loopback addresses accurately")
    void testIsLocalHostDetection() {
        assertTrue(DBConnection.isLocalHost("localhost"));
        assertTrue(DBConnection.isLocalHost("LOCALHOST"));
        assertTrue(DBConnection.isLocalHost("127.0.0.1"));
        assertTrue(DBConnection.isLocalHost("::1"));
        assertTrue(DBConnection.isLocalHost("host.docker.internal"));
        assertTrue(DBConnection.isLocalHost(null));
        assertTrue(DBConnection.isLocalHost(""));

        assertFalse(DBConnection.isLocalHost("gateway01.us-west-2.prod.aws.tidbcloud.com"));
        assertFalse(DBConnection.isLocalHost("mysql.rds.amazonaws.com"));
        assertFalse(DBConnection.isLocalHost("192.168.1.100"));
    }
}
