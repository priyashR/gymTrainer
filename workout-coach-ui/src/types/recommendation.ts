// --- Recommendation Domain Types ---

export interface ExerciseRecommendationDto {
  exerciseIndex: number;
  prescribedWeight: string | null;
  prescribedReps: string | null;
  prescribedSets: number | null;
}

export interface SectionRecommendations {
  sectionIndex: number;
  exercises: ExerciseRecommendationDto[];
}

export interface RecommendationsResponse {
  sections: SectionRecommendations[];
}
