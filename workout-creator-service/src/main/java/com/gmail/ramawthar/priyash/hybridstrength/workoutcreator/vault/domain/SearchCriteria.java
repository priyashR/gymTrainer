package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

/**
 * Value object encapsulating search and filter parameters for vault queries.
 * All fields are nullable — a null or blank value means "no filter on this dimension".
 * Pure domain object — no framework dependencies.
 */
public record SearchCriteria(
        String query,
        String focusArea,
        String modality
) {
    /**
     * Returns true if a keyword search query is present and non-blank.
     */
    public boolean hasKeyword() {
        return query != null && !query.isBlank();
    }

    /**
     * Returns true if a focus area filter is present and non-blank.
     */
    public boolean hasFocusArea() {
        return focusArea != null && !focusArea.isBlank();
    }

    /**
     * Returns true if a modality filter is present and non-blank.
     */
    public boolean hasModality() {
        return modality != null && !modality.isBlank();
    }
}
