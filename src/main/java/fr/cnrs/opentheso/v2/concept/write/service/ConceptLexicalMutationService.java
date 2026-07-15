package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLexicalNativeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptLexicalMutationService {

    private final ConceptLexicalNativeWriteService conceptLexicalNativeWriteService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    @Transactional
    public MutationResult addSynonym(AddSynonymCommand command) {
        return conceptLexicalNativeWriteService.addSynonym(command);
    }

    @Transactional
    public MutationResult updateSynonym(UpdateSynonymCommand command) {
        return conceptLexicalNativeWriteService.updateSynonym(command);
    }

    @Transactional
    public MutationResult deleteSynonym(DeleteSynonymCommand command) {
        return conceptLexicalNativeWriteService.deleteSynonym(command);
    }

    @Transactional
    public MutationResult addTranslation(AddTranslationCommand command) {
        return conceptLexicalNativeWriteService.addTranslation(command);
    }

    @Transactional
    public MutationResult updateTranslation(UpdateTranslationCommand command) {
        return conceptLexicalNativeWriteService.updateTranslation(command);
    }

    @Transactional
    public MutationResult deleteTranslation(DeleteTranslationCommand command) {
        return conceptLexicalNativeWriteService.deleteTranslation(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        return conceptWriteMetadataService.listUsedLanguages(thesaurusId, workLang);
    }
}
