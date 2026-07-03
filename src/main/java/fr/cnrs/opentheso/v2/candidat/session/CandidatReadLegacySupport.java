package fr.cnrs.opentheso.v2.candidat.session;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeImage;

import java.util.List;

public interface CandidatReadLegacySupport {

    List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId);

    List<NodeImage> loadExternalImages(String thesaurusId, String conceptId);
}
