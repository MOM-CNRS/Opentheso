package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentType;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptAlignmentMutationService {

    private final AlignmentService alignmentService;
    private final ConceptService conceptService;

    @Transactional(readOnly = true)
    public List<NodeAlignmentType> listAlignmentTypes() {
        return alignmentService.searchAllAlignementTypes();
    }

    @Transactional
    public boolean addManualAlignment(
            int userId,
            String source,
            String uri,
            int typeId,
            String conceptId,
            String thesaurusId
    ) {
        boolean saved = alignmentService.addNewAlignment(
                userId,
                "",
                source,
                uri,
                typeId,
                conceptId,
                thesaurusId,
                0
        );
        if (saved) {
            conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
        }
        return saved;
    }

    @Transactional
    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        return alignmentService.deleteAlignment(alignmentId, thesaurusId);
    }

    @Transactional
    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        alignmentService.updateAlignement(element, conceptId, thesaurusId);
    }
}
