package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.candidat.mapper.CandidatDetailMapper;
import fr.cnrs.opentheso.v2.candidat.mapper.CandidatMapper;
import fr.cnrs.opentheso.v2.candidat.session.CandidatReadLegacySupport;
import fr.cnrs.opentheso.v2.shared.repository.CandidatQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CandidatReadService {

    private final CandidatQueryRepository candidatQueryRepository;
    private final CandidatReadLegacySupport candidatReadLegacySupport;

    @Transactional(readOnly = true)
    public List<CandidatDto> loadByStatus(String thesaurusId, String lang, int statusId) {
        return searchByStatus(thesaurusId, lang, statusId, null);
    }

    @Transactional(readOnly = true)
    public List<CandidatDto> searchByStatus(String thesaurusId, String lang, int statusId, String searchTerm) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        var rows = StringUtils.isBlank(searchTerm)
                ? candidatQueryRepository.findCandidatesByStatus(thesaurusId, lang, statusId)
                : candidatQueryRepository.findCandidatesByStatus(thesaurusId, lang, statusId, searchTerm);
        return CandidatMapper.toCandidatDtos(rows, thesaurusId, statusId);
    }

    @Transactional(readOnly = true)
    public void loadDetails(CandidatDto candidat, String thesaurusId) {
        if (candidat == null || StringUtils.isBlank(candidat.getIdConcepte()) || StringUtils.isBlank(thesaurusId)) {
            return;
        }

        String conceptId = candidat.getIdConcepte();
        String lang = candidat.getLang();
        int userId = candidat.getUserId();

        var bundle = candidatQueryRepository.findCandidateDetailBundle(thesaurusId, conceptId, lang, userId)
                .orElse(null);
        if (bundle == null) {
            return;
        }

        var detail = bundle.detail();
        var alignments = candidatReadLegacySupport.loadAlignments(conceptId, thesaurusId);
        var images = candidatReadLegacySupport.loadExternalImages(thesaurusId, conceptId);

        CandidatDetailMapper.applyDetails(
                candidat,
                bundle.preferredTermId(),
                detail.collections(),
                detail.broaderRelations(),
                detail.relatedRelations(),
                detail.synonyms(),
                detail.notes(),
                detail.votedNoteIds(),
                detail.translations(),
                detail.messages(),
                bundle.voted(),
                alignments,
                images,
                userId
        );
    }

    @Transactional(readOnly = true)
    public Map<String, List<NodeIdValue>> loadBroaderTermsForConcepts(String thesaurusId, List<String> conceptIds, String lang) {
        if (StringUtils.isBlank(thesaurusId) || conceptIds == null || conceptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        var rows = candidatQueryRepository.findBroaderRelationsForConcepts(thesaurusId, conceptIds, lang);
        return CandidatDetailMapper.groupBroaderRelationsByConcept(rows);
    }

    @Transactional(readOnly = true)
    public void prepareCandidatesForAccept(List<CandidatDto> candidates, String thesaurusId, String lang) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        var broaderByConcept = loadBroaderTermsForConcepts(
                thesaurusId,
                CandidatDetailMapper.extractConceptIds(candidates),
                lang
        );
        candidates.forEach(candidate -> CandidatDetailMapper.applyBroaderTerms(candidate, broaderByConcept));
    }
}
