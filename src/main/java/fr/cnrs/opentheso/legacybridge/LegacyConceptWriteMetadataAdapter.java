package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWriteMetadataPort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LegacyConceptWriteMetadataAdapter implements ConceptWriteMetadataPort {

    private final ThesaurusService thesaurusService;
    private final NoteService noteService;
    private final RelationService relationService;
    private final ConceptTypeService conceptTypeService;

    @Override
    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        if (StringUtils.isAnyBlank(thesaurusId, workLang)) {
            return Collections.emptyList();
        }
        return thesaurusService.getAllUsedLanguagesOfThesaurusNode(thesaurusId, workLang).stream()
                .map(lang -> new ConceptWriteLanguage(lang.getCode(), lang.getValue()))
                .toList();
    }

    @Override
    public List<ConceptWriteNoteType> listNoteTypes() {
        return noteService.getNotesType().stream()
                .map(type -> new ConceptWriteNoteType(type.getCode()))
                .toList();
    }

    @Override
    public List<ConceptWriteNtRelationType> listNtRelationTypes() {
        return relationService.getTypesRelationsNT().stream()
                .map(type -> new ConceptWriteNtRelationType(
                        type.getRelationType(),
                        type.getDescriptionFr(),
                        type.getDescriptionEn()))
                .toList();
    }

    @Override
    public List<ConceptWriteConceptType> listConceptTypes(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptTypeService.getAllTypesOfConcept(thesaurusId).stream()
                .map(type -> new ConceptWriteConceptType(
                        type.getCode(),
                        type.getLabelFr(),
                        type.getLabelEn(),
                        type.isReciprocal(),
                        type.isPermanent()))
                .toList();
    }

    @Override
    public Optional<ConceptWriteNoteDraft> loadNoteDraft(
            String thesaurusId,
            String conceptId,
            String lang,
            String typeCode
    ) {
        NodeNote existing = noteService.getNodeNote(conceptId, thesaurusId, lang, typeCode);
        if (existing == null) {
            return Optional.empty();
        }
        return Optional.of(new ConceptWriteNoteDraft(
                existing.getIdNote(),
                StringUtils.defaultString(existing.getLexicalValue()),
                StringUtils.defaultString(existing.getNoteSource())
        ));
    }
}
