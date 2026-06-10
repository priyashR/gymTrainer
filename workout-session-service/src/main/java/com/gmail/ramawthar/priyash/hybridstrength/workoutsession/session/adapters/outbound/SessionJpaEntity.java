package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code sessions} table.
 * Maps JSONB columns via {@link SectionProgressListConverter}.
 */
@Entity
@Table(name = "sessions")
class SessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "current_section_index", nullable = false)
    private int currentSectionIndex;

    @Column(name = "workout_snapshot", nullable = false, columnDefinition = "jsonb")
    private String workoutSnapshot;

    @Convert(converter = SectionProgressListConverter.class)
    @Column(name = "section_progresses", nullable = false, columnDefinition = "jsonb")
    private List<SectionProgressDto> sectionProgresses;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_persisted_at", nullable = false)
    private Instant lastPersistedAt;

    @Column(name = "total_paused_seconds", nullable = false)
    private long totalPausedSeconds;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    protected SessionJpaEntity() {
        // JPA requires a no-arg constructor
    }

    static SessionJpaEntity fromDomain(Session session) {
        SessionJpaEntity entity = new SessionJpaEntity();
        entity.id = session.getId();
        entity.userId = session.getUserId();
        entity.programId = session.getProgramId();
        entity.enrollmentId = session.getEnrollmentId();
        entity.weekNumber = session.getWeekNumber();
        entity.dayNumber = session.getDayNumber();
        entity.status = session.getStatus();
        entity.currentSectionIndex = session.getCurrentSectionIndex();
        entity.workoutSnapshot = session.getWorkoutSnapshot();
        entity.sectionProgresses = session.getSectionProgresses().stream()
                .map(SectionProgressDto::fromDomain)
                .toList();
        entity.startedAt = session.getStartedAt();
        entity.pausedAt = session.getPausedAt();
        entity.completedAt = session.getCompletedAt();
        entity.lastPersistedAt = session.getLastPersistedAt();
        entity.totalPausedSeconds = session.getTotalPausedSeconds();
        entity.durationSeconds = session.getDurationSeconds();
        return entity;
    }

    Session toDomain() {
        List<SectionProgress> domainProgresses = sectionProgresses.stream()
                .map(SectionProgressDto::toDomain)
                .toList();

        return new Session.Builder()
                .id(id)
                .userId(userId)
                .programId(programId)
                .enrollmentId(enrollmentId)
                .weekNumber(weekNumber)
                .dayNumber(dayNumber)
                .status(status)
                .currentSectionIndex(currentSectionIndex)
                .sectionProgresses(domainProgresses)
                .workoutSnapshot(workoutSnapshot)
                .startedAt(startedAt)
                .pausedAt(pausedAt)
                .completedAt(completedAt)
                .lastPersistedAt(lastPersistedAt)
                .totalPausedSeconds(totalPausedSeconds)
                .durationSeconds(durationSeconds)
                .build();
    }
}
