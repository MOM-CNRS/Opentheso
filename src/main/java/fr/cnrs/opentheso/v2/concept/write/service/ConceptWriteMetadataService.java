package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWriteMetadataPort;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptWriteMetadataService {

    private final ConceptWriteMetadataPort conceptWriteMetadataPort;
    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        return conceptWriteMetadataPort.listUsedLanguages(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNoteType> listNoteTypes() {
        return conceptWriteMetadataPort.listNoteTypes();
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNtRelationType> listNtRelationTypes() {
        return conceptWriteMetadataPort.listNtRelationTypes();
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteConceptType> listConceptTypes(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptWriteMetadataPort.listConceptTypes(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteCollection> listCollections(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return conceptQueryRepository.findConceptGroups(thesaurusId, lang).stream()
                .map(row -> new ConceptWriteCollection(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 2)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ConceptWriteNoteDraft> loadNoteDraft(
            String thesaurusId,
            String conceptId,
            String lang,
            String typeCode
    ) {
        return conceptWriteMetadataPort.loadNoteDraft(thesaurusId, conceptId, lang, typeCode);
    }
}
