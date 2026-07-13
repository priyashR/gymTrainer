package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DaySummary;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for browsing available days within a vault program.
 * Returns lightweight day summaries for the copy-day picker UI.
 */
public interface GetProgramDaysUseCase {

    List<DaySummary> getProgramDays(UUID programId, String ownerUserId);
}
