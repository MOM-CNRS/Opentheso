package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptLexicalMutationService {

    private final ConceptWritePort conceptWritePort;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    @Transactional
    public MutationResult addSynonym(AddSynonymCommand command) {
        return conceptWritePort.addSynonym(command);
    }

    @Transactional
    public MutationResult updateSynonym(UpdateSynonymCommand command) {
        return conceptWritePort.updateSynonym(command);
    }

    @Transactional
    public MutationResult deleteSynonym(DeleteSynonymCommand command) {
        return conceptWritePort.deleteSynonym(command);
    }

    @Transactional
    public MutationResult addTranslation(AddTranslationCommand command) {
        return conceptWritePort.addTranslation(command);
    }

    @Transactional
    public MutationResult updateTranslation(UpdateTranslationCommand command) {
        return conceptWritePort.updateTranslation(command);
    }

    @Transactional
    public MutationResult deleteTranslation(DeleteTranslationCommand command) {
        return conceptWritePort.deleteTranslation(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        return conceptWriteMetadataService.listUsedLanguages(thesaurusId, workLang);
    }
}
