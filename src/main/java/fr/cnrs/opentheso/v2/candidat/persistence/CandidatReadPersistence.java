package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidatReadPersistence {

    private final AlignementRepository alignementRepository;
    private final ImagesRepository imagesRepository;

    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        var alignements = alignementRepository.findAllAlignmentsByConceptAndThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isEmpty(alignements)) {
            return List.of();
        }
        return alignements.stream()
                .map(element -> NodeAlignment.builder()
                        .id_alignement(element.getId())
                        .created(element.getCreated())
                        .modified(element.getModified())
                        .id_author(element.getAuthor())
                        .thesaurus_target(element.getThesaurus_target())
                        .concept_target(element.getConcept_target())
                        .uri_target(element.getUri_target())
                        .alignement_id_type(element.getAlignement_id_type())
                        .internal_id_concept(element.getInternal_id_concept())
                        .internal_id_thesaurus(element.getInternal_id_thesaurus())
                        .alignmentLabelType(element.getLabel())
                        .alignmentLabelSkosType(element.getLabel_skos())
                        .alignementLocalValide(element.getUrl_available())
                        .id_source(element.getId_alignement_source() == null ? 0 : element.getId_alignement_source())
                        .build())
                .toList();
    }

    public List<NodeImage> loadExternalImages(String thesaurusId, String conceptId) {
        var result = imagesRepository.findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        return CollectionUtils.isNotEmpty(result)
                ? result.stream()
                        .map(element -> new NodeImage(
                                element.getId(),
                                element.getIdConcept(),
                                element.getIdThesaurus(),
                                element.getImageName(),
                                element.getImageCreator(),
                                element.getImageCopyright(),
                                element.getExternalUri(),
                                ""))
                        .toList()
                : List.of();
    }
}
