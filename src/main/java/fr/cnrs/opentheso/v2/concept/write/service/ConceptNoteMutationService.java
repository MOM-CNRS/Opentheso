package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptNoteMutationService {

    private final ConceptWritePort conceptWritePort;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    @Transactional
    public MutationResult upsertNote(UpsertNoteCommand command) {
        return conceptWritePort.upsertNote(command);
    }

    @Transactional
    public MutationResult deleteNote(DeleteNoteCommand command) {
        return conceptWritePort.deleteNote(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNoteType> listNoteTypes() {
        return conceptWriteMetadataService.listNoteTypes();
    }
}
