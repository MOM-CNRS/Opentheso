package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWriteMetadataPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptWriteMetadataService {

    private final ConceptWriteMetadataPersistence conceptWriteMetadataPersistence;
    private final GroupService groupService;

    @Transactional(readOnly = true)
    public List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang) {
        return conceptWriteMetadataPersistence.listUsedLanguages(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNoteType> listNoteTypes() {
        return conceptWriteMetadataPersistence.listNoteTypes();
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNtRelationType> listNtRelationTypes() {
        return conceptWriteMetadataPersistence.listNtRelationTypes();
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteConceptType> listConceptTypes(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptWriteMetadataPersistence.listConceptTypes(thesaurusId);
    }

    /**
     * Même source que legacy ({@link GroupService#getListConceptGroup}) avec déduplication stricte par id.
     */
    @Transactional(readOnly = true)
    public List<ConceptWriteCollection> listCollections(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, ConceptWriteCollection> unique = new LinkedHashMap<>();
        List<NodeGroup> groups = groupService.getListConceptGroup(thesaurusId, lang);
        if (groups == null) {
            return Collections.emptyList();
        }
        for (NodeGroup group : groups) {
            if (group == null || group.getConceptGroup() == null
                    || StringUtils.isBlank(group.getConceptGroup().getIdGroup())) {
                continue;
            }
            String id = group.getConceptGroup().getIdGroup();
            String label = StringUtils.defaultIfBlank(group.getLexicalValue(), id);
            unique.putIfAbsent(id.toLowerCase(), new ConceptWriteCollection(id, label));
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(
                        collection -> StringUtils.defaultString(collection.label()),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ConceptWriteNoteDraft> loadNoteDraft(
            String thesaurusId,
            String conceptId,
            String lang,
            String typeCode
    ) {
        return conceptWriteMetadataPersistence.loadNoteDraft(thesaurusId, conceptId, lang, typeCode);
    }
}
