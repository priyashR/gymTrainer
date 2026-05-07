package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProgramJpaEntity}.
 * Public so the upload adapter in a sibling package can reuse it without duplicating
 * the entity infrastructure.
 */
public interface ProgramSpringDataRepository extends JpaRepository<ProgramJpaEntity, UUID> {

    Page<ProgramJpaEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);

    Optional<ProgramJpaEntity> findByIdAndOwnerUserId(UUID id, String ownerUserId);

    void deleteByIdAndOwnerUserId(UUID id, String ownerUserId);

    boolean existsByIdAndOwnerUserId(UUID id, String ownerUserId);

    /**
     * Search programs by keyword (name/goal), focus area, and modality with relevance ordering.
     * Name matches rank higher than goal-only matches; ties broken by createdAt descending.
     *
     * <p>All filter parameters are nullable — when null they are excluded from the WHERE clause.
     */
    @Query("""
            SELECT DISTINCT p FROM ProgramJpaEntity p
            LEFT JOIN p.weeks w
            LEFT JOIN w.days d
            WHERE p.ownerUserId = :ownerUserId
            AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.goal) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:focusArea IS NULL OR LOWER(d.focusArea) = LOWER(:focusArea))
            AND (:modality IS NULL OR LOWER(CAST(d.modality AS string)) = LOWER(:modality))
            ORDER BY
                CASE WHEN :query IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                     THEN 0 ELSE 1 END,
                p.createdAt DESC
            """)
    Page<ProgramJpaEntity> searchPrograms(
            @Param("ownerUserId") String ownerUserId,
            @Param("query") String query,
            @Param("focusArea") String focusArea,
            @Param("modality") String modality,
            Pageable pageable);
}
