package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto;

import java.util.List;

/**
 * Response body for a successful program upload (201 Created).
 */
public record UploadProgramResponse(
        String id,
        String programName,
        int durationWeeks,
        String goal,
        List<String> equipmentProfile,
        String contentSource,
        String createdAt
) {}
