package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.repositories.NoteTypeRepository;
import fr.cnrs.opentheso.repositories.NtTypeRepository;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.setting.mapper.SettingMapper;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConceptWriteMetadataPersistence {

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    private final NoteTypeRepository noteTypeRepository;
    private final NtTypeRepository ntTypeRepository;
    private final ConceptTypeRepository conceptTypeRepository;
    private final ConceptNoteWriteRepository conceptNoteWriteRepository;

    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        if (StringUtils.isAnyBlank(thesaurusId, workLang)) {
            return Collections.emptyList();
        }
        return thesaurusSettingsQueryRepository.findUsedLanguages(thesaurusId, workLang).stream()
                .map(SettingMapper::toLanguage)
                .map(lang -> new ConceptWriteLanguage(lang.code(), lang.displayLabel()))
                .toList();
    }

    public List<ConceptWriteNoteType> listNoteTypes() {
        return noteTypeRepository.findAll().stream()
                .map(type -> new ConceptWriteNoteType(type.getCode()))
                .toList();
    }

    public List<ConceptWriteNtRelationType> listNtRelationTypes() {
        return ntTypeRepository.findAll().stream()
                .map(type -> new ConceptWriteNtRelationType(
                        type.getRelation(),
                        type.getDescriptionFr(),
                        type.getDescriptionEn()))
                .toList();
    }

    public List<ConceptWriteConceptType> listConceptTypes(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptTypeRepository.findAllByIdThesaurusIn(List.of(thesaurusId)).stream()
                .map(this::toConceptWriteType)
                .toList();
    }

    public Optional<ConceptWriteNoteDraft> loadNoteDraft(
            String thesaurusId,
            String conceptId,
            String lang,
            String typeCode
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang, typeCode)) {
            return Optional.empty();
        }
        Optional<Integer> noteId = conceptNoteWriteRepository.findNoteId(conceptId, thesaurusId, lang, typeCode);
        Optional<String> lexicalValue = conceptNoteWriteRepository.findNoteLexicalValue(conceptId, thesaurusId, lang, typeCode);
        if (noteId.isEmpty() && lexicalValue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConceptWriteNoteDraft(
                noteId.orElse(0),
                lexicalValue.orElse(""),
                conceptNoteWriteRepository.findNoteSource(conceptId, thesaurusId, lang, typeCode).orElse("")
        ));
    }

    private ConceptWriteConceptType toConceptWriteType(ConceptType type) {
        return new ConceptWriteConceptType(
                type.getCode(),
                type.getLabelFr(),
                type.getLabelEn(),
                type.isReciprocal(),
                false
        );
    }
}
