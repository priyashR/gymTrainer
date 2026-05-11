package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadedProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.outbound.UploadProgramRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramEntityMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramSpringDataRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Outbound adapter: persists an {@link UploadedProgram} using the existing JPA entity
 * infrastructure from the {@code vault} package.
 *
 * <p>Always sets {@code content_source = UPLOADED} on every save, regardless of what
 * the domain object carries — this is an invariant of the upload flow.
 *
 * <p>After persistence the adapter reconstructs an {@link UploadedProgram} from the
 * saved entity so the application service can build the full response without touching
 * JPA types.
 */
@Repository
public class JpaUploadProgramRepository implements UploadProgramRepository {

    private final ProgramSpringDataRepository programRepo;

    public JpaUploadProgramRepository(ProgramSpringDataRepository programRepo) {
        this.programRepo = programRepo;
    }

    @Override
    public UploadedProgram save(UploadedProgram uploadedProgram) {
        ProgramJpaEntity entity = toEntity(uploadedProgram);
        ProgramJpaEntity saved = programRepo.save(entity);
        return toDomain(saved);
    }

    // -------------------------------------------------------------------------
    // Domain → Entity (upload-specific: sets UPLOADED content source and timestamps)
    // -------------------------------------------------------------------------

    private ProgramJpaEntity toEntity(UploadedProgram uploadedProgram) {
        Program program = uploadedProgram.program();
        Instant now = Instant.now();

        ProgramJpaEntity entity = ProgramEntityMapper.toEntity(program);
        entity.setOwnerUserId(uploadedProgram.ownerUserId());
        entity.setContentSource(ContentSource.UPLOADED); // invariant: always UPLOADED
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return entity;
    }

    // -------------------------------------------------------------------------
    // Entity → Domain (upload-specific: wraps as UploadedProgram)
    // -------------------------------------------------------------------------

    private UploadedProgram toDomain(ProgramJpaEntity entity) {
        Program program = ProgramEntityMapper.toProgram(entity);

        return new UploadedProgram(
                entity.getId().toString(),
                program,
                entity.getOwnerUserId(),
                entity.getContentSource(),
                entity.getCreatedAt()
        );
    }
}
