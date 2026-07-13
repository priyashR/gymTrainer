package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import java.util.UUID;

/**
 * Inbound port for creating a manual program.
 * The application service implements this interface to handle the creation logic.
 */
public interface CreateManualProgramUseCase {

    UUID createManualProgram(CreateManualProgramCommand command);
}
