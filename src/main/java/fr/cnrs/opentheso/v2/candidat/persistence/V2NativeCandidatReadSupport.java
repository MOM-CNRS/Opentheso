package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.v2.candidat.session.CandidatReadLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatReadSupport implements CandidatReadLegacySupport {

    private final CandidatReadPersistence candidatReadPersistence;

    @Override
    public java.util.List<fr.cnrs.opentheso.models.alignment.NodeAlignment> loadAlignments(
            String conceptId, String thesaurusId) {
        return candidatReadPersistence.loadAlignments(conceptId, thesaurusId);
    }

    @Override
    public java.util.List<fr.cnrs.opentheso.models.nodes.NodeImage> loadExternalImages(
            String thesaurusId, String conceptId) {
        return candidatReadPersistence.loadExternalImages(thesaurusId, conceptId);
    }
}
