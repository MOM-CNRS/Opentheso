package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ImageService;
import fr.cnrs.opentheso.v2.candidat.session.CandidatReadLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyCandidatReadSupport implements CandidatReadLegacySupport {

    private final AlignmentService alignmentService;
    private final ImageService imageService;

    @Override
    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return alignmentService.getAllAlignmentOfConcept(conceptId, thesaurusId);
    }

    @Override
    public List<NodeImage> loadExternalImages(String thesaurusId, String conceptId) {
        return imageService.getAllExternalImages(thesaurusId, conceptId);
    }
}
