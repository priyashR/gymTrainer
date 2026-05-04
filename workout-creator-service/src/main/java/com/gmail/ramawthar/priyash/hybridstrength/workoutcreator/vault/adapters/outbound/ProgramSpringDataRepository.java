package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProgramJpaEntity}.
 * Public so the upload adapter in a sibling package can reuse it without duplicating
 * the entity infrastructure.
 */
public interface ProgramSpringDataRepository extends JpaRepository<ProgramJpaEntity, UUID> {}
