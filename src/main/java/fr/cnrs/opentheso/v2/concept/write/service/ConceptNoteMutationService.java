package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptNoteNativeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptNoteMutationService {

    private final ConceptNoteNativeWriteService conceptNoteNativeWriteService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    @Transactional
    public MutationResult upsertNote(UpsertNoteCommand command) {
        return conceptNoteNativeWriteService.upsertNote(command);
    }

    @Transactional
    public MutationResult deleteNote(DeleteNoteCommand command) {
        return conceptNoteNativeWriteService.deleteNote(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNoteType> listNoteTypes() {
        return conceptWriteMetadataService.listNoteTypes();
    }
}
