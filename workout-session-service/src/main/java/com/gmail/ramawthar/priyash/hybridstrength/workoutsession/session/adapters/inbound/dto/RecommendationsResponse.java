package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for the recommendations endpoint.
 * Groups exercise recommendations by section index.
 */
public record RecommendationsResponse(
        List<SectionRecommendations> sections
) {

    public record SectionRecommendations(
            int sectionIndex,
            List<ExerciseRecommendationDto> exercises
    ) {}

    public record ExerciseRecommendationDto(
            int exerciseIndex,
            String prescribedWeight,
            String prescribedReps,
            Integer prescribedSets
    ) {}

    /**
     * Maps a flat list of domain ExerciseRecommendation objects into a grouped response DTO.
     */
    public static RecommendationsResponse from(List<ExerciseRecommendation> recommendations) {
        List<SectionRecommendations> sections = recommendations.stream()
                .collect(Collectors.groupingBy(ExerciseRecommendation::sectionIndex))
                .entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey()))
                .map(entry -> new SectionRecommendations(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparingInt(ExerciseRecommendation::exerciseIndex))
                                .map(rec -> new ExerciseRecommendationDto(
                                        rec.exerciseIndex(),
                                        rec.prescribedWeight(),
                                        rec.prescribedReps(),
                                        rec.prescribedSets()
                                ))
                                .toList()
                ))
                .toList();

        return new RecommendationsResponse(sections);
    }
}
