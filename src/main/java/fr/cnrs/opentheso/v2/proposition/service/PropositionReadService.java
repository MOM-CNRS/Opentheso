package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.models.PropositionProjection;
import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropositionReadService {

    /** Jamais ouverte / consultée. Une consultation passe le statut à {@code LU}. */
    static final String NEW_STATUS = PropositionStatusEnum.ENVOYER.name();

    private final PropositionModificationRepository propositionModificationRepository;

    @Transactional(readOnly = true)
    public int countPending(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        return (int) propositionModificationRepository
                .countByIdThesoAndStatus(thesaurusId, NEW_STATUS);
    }

    @Transactional(readOnly = true)
    public int countAll(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        return (int) propositionModificationRepository.countByIdTheso(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<PropositionSummary> listPending(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return mappedForThesaurus(thesaurusId,
                propositionModificationRepository.findAllPropositionsByStatusAndTheso(
                        NEW_STATUS, thesaurusId));
    }

    @Transactional(readOnly = true)
    public List<PropositionSummary> listAll(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return mappedForThesaurus(thesaurusId,
                propositionModificationRepository.findAllPropositionsByTheso(thesaurusId));
    }

    @Transactional(readOnly = true)
    public PropositionDetail findDetail(int propositionId) {
        List<PropositionProjection> rows = propositionModificationRepository.findProjectionsById(propositionId);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return toDetail(rows.get(0));
    }

    private List<PropositionSummary> mappedForThesaurus(
            String thesaurusId, List<PropositionProjection> projections) {
        return projections.stream()
                .map(this::toSummary)
                .filter(summary -> thesaurusId.equalsIgnoreCase(
                        StringUtils.defaultString(summary.thesaurusId())))
                .toList();
    }

    private PropositionSummary toSummary(PropositionProjection projection) {
        return new PropositionSummary(
                projection.getId(),
                projection.getIdTheso(),
                projection.getIdConcept(),
                projection.getLexicalValue(),
                projection.getNom(),
                projection.getEmail(),
                projection.getStatus(),
                projection.getDate(),
                projection.getLang(),
                projection.getCodePays()
        );
    }

    private PropositionDetail toDetail(PropositionProjection projection) {
        return new PropositionDetail(
                projection.getId(),
                projection.getIdTheso(),
                projection.getIdConcept(),
                projection.getLexicalValue(),
                projection.getLang(),
                projection.getCodePays(),
                projection.getNom(),
                projection.getEmail(),
                projection.getCommentaire(),
                projection.getStatus(),
                projection.getDate(),
                projection.getApprouvePar(),
                projection.getAdminComment()
        );
    }
}
