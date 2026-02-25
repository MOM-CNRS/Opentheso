package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class ConceptTypeService {

    private final ConceptTypeRepository conceptTypeRepository;


    public boolean updateConceptType(String idThesaurus, NodeConceptType nodeConceptType) {

        var conceptType = conceptTypeRepository.findByCodeAndIdThesaurus(nodeConceptType.getCode(), idThesaurus);
        if (conceptType.isEmpty()) {
            log.error("Aucun concept type n'est trouvé avec le code {}", conceptType);
            return false;
        }

        conceptType.get().setLabelFr(nodeConceptType.getLabelFr());
        conceptType.get().setLabelEn(nodeConceptType.getLabelEn());
        conceptType.get().setReciprocal(nodeConceptType.isReciprocal());
        conceptType.get().setIdThesaurus(idThesaurus);
        conceptTypeRepository.save(conceptType.get());
        return true;
    }

    public void deleteConceptType(String idThesaurus, NodeConceptType nodeConceptType) {
        log.debug("Suppression du concept type {}", nodeConceptType.getCode());
        conceptTypeRepository.deleteByCodeAndIdThesaurus(nodeConceptType.getCode(), idThesaurus);
    }

    public void addNewConceptType(String idThesaurus, NodeConceptType nodeConceptType) {

        log.debug("Enregistrement d'un nouveau concept type {}", nodeConceptType.getCode());
        conceptTypeRepository.save(ConceptType.builder()
                .code(nodeConceptType.getCode())
                .labelEn(nodeConceptType.getLabelEn())
                .labelFr(nodeConceptType.getLabelFr())
                .reciprocal(nodeConceptType.isReciprocal())
                .idThesaurus(idThesaurus)
                .build());
    }

    public boolean isConceptTypeExist(String idThesaurus, NodeConceptType nodeConceptType) {

        log.debug("Vérifier si le concept type {} existe", nodeConceptType.getCode());
        var conceptType = conceptTypeRepository.findByCodeAndIdThesaurus(nodeConceptType.getCode(), idThesaurus);
        return conceptType.isPresent();
    }

    public List<NodeConceptType> getAllTypesOfConcept(String idThesaurus) {

        var conceptTypes = conceptTypeRepository.findAllByIdThesaurusIn(List.of(idThesaurus, "all"));
        if (CollectionUtils.isEmpty(conceptTypes)) {
            log.error("Aucun concept type n'est trouvé");
            return List.of();
        }
        return conceptTypes.stream()
                .map(conceptType -> NodeConceptType.builder()
                        .code(conceptType.getCode())
                        .labelEn(conceptType.getLabelEn())
                        .labelFr(conceptType.getLabelFr())
                        .permanent(conceptType.isReciprocal())
                        .permanent("all".equalsIgnoreCase(conceptType.getIdThesaurus()))
                        .build())
                .toList();
    }

    public NodeConceptType getNodeTypeConcept(String conceptType, String idThesaurus) {

        log.debug("Recherche du concept type {} dans le thésaurus {}", conceptType, idThesaurus);
        var result = conceptTypeRepository.findByCodeAndIdThesaurusIn(conceptType, List.of(idThesaurus, "all"));
        if (result.isEmpty()) {
            log.debug("Aucune concept type n'est trouvé avec le code {} dans le thésaurus {}", conceptType, idThesaurus);
            return null;
        }
        return NodeConceptType.builder()
                .code(conceptType)
                .labelFr(result.get().getLabelFr())
                .labelEn(result.get().getLabelEn())
                .reciprocal(result.get().isReciprocal())
                .build();
    }

    public String getLabelOfTypeConcept(String codeType, String idThesaurus, String idLang) {

        var conceptType = getNodeTypeConcept(codeType, idThesaurus);
        if (conceptType == null) {
            return "";
        }
        return "fr".equalsIgnoreCase(idLang) ? conceptType.getLabelFr() : conceptType.getLabelEn();
    }

}
