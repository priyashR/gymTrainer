package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;

import java.util.List;

/**
 * DTO for JSON serialization of section progress data stored in the JSONB column.
 * Package-private — used only by the persistence layer.
 */
class SectionProgressDto {

    private int sectionIndex;
    private String sectionName;
    private String sectionType;
    private List<ExerciseLogDto> exerciseLogs;
    private boolean completed;
    private CrossFitScoreDto crossFitScore;
    private int roundCount;

    // No-arg constructor for Jackson
    SectionProgressDto() {
    }

    SectionProgressDto(int sectionIndex, String sectionName, String sectionType,
                       List<ExerciseLogDto> exerciseLogs, boolean completed,
                       CrossFitScoreDto crossFitScore, int roundCount) {
        this.sectionIndex = sectionIndex;
        this.sectionName = sectionName;
        this.sectionType = sectionType;
        this.exerciseLogs = exerciseLogs;
        this.completed = completed;
        this.crossFitScore = crossFitScore;
        this.roundCount = roundCount;
    }

    static SectionProgressDto fromDomain(SectionProgress domain) {
        List<ExerciseLogDto> logs = domain.getExerciseLogs().stream()
                .map(ExerciseLogDto::fromDomain)
                .toList();
        CrossFitScoreDto scoreDto = CrossFitScoreDto.fromDomain(domain.getCrossFitScore());
        return new SectionProgressDto(
                domain.getSectionIndex(),
                domain.getSectionName(),
                domain.getSectionType().name(),
                logs,
                domain.isCompleted(),
                scoreDto,
                domain.getRoundCount()
        );
    }

    SectionProgress toDomain() {
        List<ExerciseLog> logs = exerciseLogs.stream()
                .map(ExerciseLogDto::toDomain)
                .toList();
        CrossFitScore domainScore = (crossFitScore != null) ? crossFitScore.toDomain() : null;
        return new SectionProgress(
                sectionIndex,
                sectionName,
                SectionType.valueOf(sectionType),
                logs,
                completed,
                domainScore,
                roundCount
        );
    }

    // --- Getters and setters for Jackson ---

    public int getSectionIndex() {
        return sectionIndex;
    }

    public void setSectionIndex(int sectionIndex) {
        this.sectionIndex = sectionIndex;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public List<ExerciseLogDto> getExerciseLogs() {
        return exerciseLogs;
    }

    public void setExerciseLogs(List<ExerciseLogDto> exerciseLogs) {
        this.exerciseLogs = exerciseLogs;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public CrossFitScoreDto getCrossFitScore() {
        return crossFitScore;
    }

    public void setCrossFitScore(CrossFitScoreDto crossFitScore) {
        this.crossFitScore = crossFitScore;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }
}
