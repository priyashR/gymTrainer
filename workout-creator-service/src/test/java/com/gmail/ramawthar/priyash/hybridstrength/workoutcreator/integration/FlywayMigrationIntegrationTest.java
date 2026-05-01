package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies all Flyway migrations V100–V104 apply cleanly
 * against the dev PostgreSQL instance and that the resulting schema has the
 * expected tables, columns, foreign keys, and indexes.
 *
 * Requirements: 7.2, 7.3, 7.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class FlywayMigrationIntegrationTest {

    @DynamicPropertySource
    static void overrideGeminiUrl(DynamicPropertyRegistry registry) {
        registry.add("gemini.base-url", () -> "http://localhost:9999");
        registry.add("gemini.api-key", () -> "test-api-key");
    }

    /**
     * Override the RSAPublicKey bean so the application context starts without
     * needing a real auth-service key. The Flyway test does not exercise JWT auth.
     */
    @Configuration
    static class TestSecurityConfig {
        private static final TestJwtHelper JWT_HELPER = new TestJwtHelper();

        @Bean
        @Primary
        RSAPublicKey rsaPublicKey() {
            return JWT_HELPER.getPublicKey();
        }
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    // ---- Table existence ----

    @Test
    void workoutsTableExists() {
        assertTableExists("workouts");
    }

    @Test
    void sectionsTableExists() {
        assertTableExists("sections");
    }

    @Test
    void exercisesTableExists() {
        assertTableExists("exercises");
    }

    @Test
    void programsTableExists() {
        assertTableExists("programs");
    }

    @Test
    void programWorkoutsTableExists() {
        assertTableExists("program_workouts");
    }

    // ---- Column presence ----

    @Test
    void workoutsTableHasExpectedColumns() {
        List<String> columns = getColumnNames("workouts");
        assertThat(columns).contains(
                "id", "user_id", "name", "description",
                "training_style", "raw_gemini_response", "created_at", "updated_at"
        );
    }

    @Test
    void sectionsTableHasExpectedColumns() {
        List<String> columns = getColumnNames("sections");
        assertThat(columns).contains(
                "id", "workout_id", "name", "section_type", "sort_order",
                "time_cap_minutes", "interval_seconds", "total_rounds",
                "work_interval_seconds", "rest_interval_seconds", "created_at"
        );
    }

    @Test
    void exercisesTableHasExpectedColumns() {
        List<String> columns = getColumnNames("exercises");
        assertThat(columns).contains(
                "id", "section_id", "name", "sets", "reps",
                "weight", "rest_seconds", "sort_order", "created_at"
        );
    }

    @Test
    void programsTableHasExpectedColumns() {
        List<String> columns = getColumnNames("programs");
        assertThat(columns).contains(
                "id", "user_id", "name", "description", "scope",
                "training_styles", "raw_gemini_response", "created_at", "updated_at"
        );
    }

    @Test
    void programWorkoutsTableHasExpectedColumns() {
        List<String> columns = getColumnNames("program_workouts");
        assertThat(columns).contains("id", "program_id", "workout_id", "day_number");
    }

    // ---- Foreign key constraints ----

    @Test
    void sectionsHasForeignKeyToWorkouts() {
        assertForeignKeyExists("sections", "workouts");
    }

    @Test
    void exercisesHasForeignKeyToSections() {
        assertForeignKeyExists("exercises", "sections");
    }

    @Test
    void programWorkoutsHasForeignKeyToPrograms() {
        assertForeignKeyExists("program_workouts", "programs");
    }

    @Test
    void programWorkoutsHasForeignKeyToWorkouts() {
        assertForeignKeyExists("program_workouts", "workouts");
    }

    // ---- Indexes ----

    @Test
    void workoutsHasUserIdIndex() {
        assertIndexExists("idx_workouts_user_id");
    }

    @Test
    void sectionsHasWorkoutIdIndex() {
        assertIndexExists("idx_sections_workout_id");
    }

    @Test
    void exercisesHasSectionIdIndex() {
        assertIndexExists("idx_exercises_section_id");
    }

    @Test
    void programsHasUserIdIndex() {
        assertIndexExists("idx_programs_user_id");
    }

    @Test
    void programWorkoutsHasProgramIdIndex() {
        assertIndexExists("idx_program_workouts_program_id");
    }

    // ---- Unique constraint on program_workouts ----

    @Test
    void programWorkoutsHasUniqueConstraintOnProgramIdAndDayNumber() {
        String sql = """
                SELECT COUNT(*) FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu
                    ON tc.constraint_name = ccu.constraint_name
                    AND tc.table_schema = ccu.table_schema
                WHERE tc.constraint_type = 'UNIQUE'
                  AND tc.table_name = 'program_workouts'
                  AND ccu.column_name IN ('program_id', 'day_number')
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    // ---- Flyway schema history ----

    @Test
    void allMigrationsAppliedSuccessfully() {
        String sql = """
                SELECT script, success FROM flyway_schema_history
                WHERE script LIKE 'V1%'
                ORDER BY installed_rank
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        assertThat(rows).hasSize(5);
        rows.forEach(row ->
                assertThat((Boolean) row.get("success"))
                        .as("Migration %s should be successful", row.get("script"))
                        .isTrue()
        );
    }

    @Test
    void migrationsAreInExpectedOrder() {
        String sql = """
                SELECT script FROM flyway_schema_history
                WHERE script LIKE 'V1%'
                ORDER BY installed_rank
                """;
        List<String> scripts = jdbcTemplate.queryForList(sql, String.class);

        assertThat(scripts).containsExactly(
                "V100__create_workouts_table.sql",
                "V101__create_sections_table.sql",
                "V102__create_exercises_table.sql",
                "V103__create_programs_table.sql",
                "V104__create_program_workouts_table.sql"
        );
    }

    // ---- Helpers ----

    private void assertTableExists(String tableName) {
        String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        assertThat(count).as("Table '%s' should exist", tableName).isEqualTo(1);
    }

    private List<String> getColumnNames(String tableName) {
        String sql = """
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                ORDER BY ordinal_position
                """;
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    private void assertForeignKeyExists(String fromTable, String toTable) {
        String sql = """
                SELECT COUNT(*) FROM information_schema.referential_constraints rc
                JOIN information_schema.table_constraints tc
                    ON rc.constraint_name = tc.constraint_name
                    AND rc.constraint_schema = tc.table_schema
                JOIN information_schema.table_constraints tc2
                    ON rc.unique_constraint_name = tc2.constraint_name
                    AND rc.unique_constraint_schema = tc2.table_schema
                WHERE tc.table_name = ? AND tc2.table_name = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fromTable, toTable);
        assertThat(count)
                .as("Table '%s' should have a foreign key referencing '%s'", fromTable, toTable)
                .isGreaterThanOrEqualTo(1);
    }

    private void assertIndexExists(String indexName) {
        String sql = """
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, indexName);
        assertThat(count).as("Index '%s' should exist", indexName).isEqualTo(1);
    }
}
