package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchCriteria} value object.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
class SearchCriteriaTest {

    // ── hasKeyword ────────────────────────────────────────────────────────────

    @Test
    void hasKeyword_NullQuery_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria(null, null, null);
        assertThat(criteria.hasKeyword()).isFalse();
    }

    @Test
    void hasKeyword_EmptyQuery_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("", null, null);
        assertThat(criteria.hasKeyword()).isFalse();
    }

    @Test
    void hasKeyword_BlankQuery_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("   ", null, null);
        assertThat(criteria.hasKeyword()).isFalse();
    }

    @Test
    void hasKeyword_ValidQuery_ReturnsTrue() {
        SearchCriteria criteria = new SearchCriteria("strength", null, null);
        assertThat(criteria.hasKeyword()).isTrue();
    }

    @Test
    void hasKeyword_QueryWithLeadingTrailingSpaces_ReturnsTrue() {
        SearchCriteria criteria = new SearchCriteria("  strength  ", null, null);
        assertThat(criteria.hasKeyword()).isTrue();
    }

    // ── hasFocusArea ──────────────────────────────────────────────────────────

    @Test
    void hasFocusArea_NullFocusArea_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", null, null);
        assertThat(criteria.hasFocusArea()).isFalse();
    }

    @Test
    void hasFocusArea_EmptyFocusArea_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", "", null);
        assertThat(criteria.hasFocusArea()).isFalse();
    }

    @Test
    void hasFocusArea_BlankFocusArea_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", "   ", null);
        assertThat(criteria.hasFocusArea()).isFalse();
    }

    @Test
    void hasFocusArea_ValidFocusArea_ReturnsTrue() {
        SearchCriteria criteria = new SearchCriteria("query", "Push", null);
        assertThat(criteria.hasFocusArea()).isTrue();
    }

    // ── hasModality ───────────────────────────────────────────────────────────

    @Test
    void hasModality_NullModality_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", null, null);
        assertThat(criteria.hasModality()).isFalse();
    }

    @Test
    void hasModality_EmptyModality_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", null, "");
        assertThat(criteria.hasModality()).isFalse();
    }

    @Test
    void hasModality_BlankModality_ReturnsFalse() {
        SearchCriteria criteria = new SearchCriteria("query", null, "   ");
        assertThat(criteria.hasModality()).isFalse();
    }

    @Test
    void hasModality_ValidModality_ReturnsTrue() {
        SearchCriteria criteria = new SearchCriteria("query", null, "CrossFit");
        assertThat(criteria.hasModality()).isTrue();
    }

    // ── combined ──────────────────────────────────────────────────────────────

    @Test
    void allFilters_AllNull_AllReturnFalse() {
        SearchCriteria criteria = new SearchCriteria(null, null, null);
        assertThat(criteria.hasKeyword()).isFalse();
        assertThat(criteria.hasFocusArea()).isFalse();
        assertThat(criteria.hasModality()).isFalse();
    }

    @Test
    void allFilters_AllPopulated_AllReturnTrue() {
        SearchCriteria criteria = new SearchCriteria("strength", "Push", "Hypertrophy");
        assertThat(criteria.hasKeyword()).isTrue();
        assertThat(criteria.hasFocusArea()).isTrue();
        assertThat(criteria.hasModality()).isTrue();
    }
}
