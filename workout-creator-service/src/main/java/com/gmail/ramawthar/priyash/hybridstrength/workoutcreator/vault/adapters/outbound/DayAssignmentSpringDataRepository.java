package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DayAssignmentJpaEntity}.
 * Used by the JPA adapter to batch-persist day assignments for manual programs.
 */
public interface DayAssignmentSpringDataRepository extends JpaRepository<DayAssignmentJpaEntity, UUID> {
    List<DayAssignmentJpaEntity> findByProgramIdOrderByDayNumberAsc(UUID programId);
}
