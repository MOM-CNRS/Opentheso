package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.CorpusLink;
import fr.cnrs.opentheso.models.nodes.NodeCorpus;
import fr.cnrs.opentheso.repositories.CorpusLinkRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class CorpusService {

    private final CorpusLinkRepository corpusLinkRepository;


    public List<NodeCorpus> getAllCorpusByThesaurus(String idThesaurus) {

        log.debug("Recherche des Corpus liés au thésaurus id {}", idThesaurus);
        var corpusList = corpusLinkRepository.findAllByIdThesaurusOrderBySortAsc(idThesaurus);
        if (corpusList.isEmpty()) {
            log.debug("Aucun corpus n'est trouvé dans le thésaurus id {}", idThesaurus);
            return List.of();
        }

        log.debug("{} corpus liés au thésaurus id {} trouvés", corpusList.size(), idThesaurus);
        return corpusList.stream()
                .map(element -> NodeCorpus.builder()
                        .corpusName(element.getCorpusName())
                        .active(element.isActive())
                        .omekaS(element.isOmekaS())
                        .isOnlyUriLink(element.isOnlyUriLink())
                        .uriLink(element.getUriLink())
                        .uriCount(element.getUriCount())
                        .build())
                .toList();
    }

    public CorpusLink getCorpusByNameAndThesaurus(String name, String idThesaurus) {

        log.debug("Recherche de corpus {} dans le thésaurus {}", name, idThesaurus);
        var corpus = corpusLinkRepository.findByIdThesaurusAndCorpusName(idThesaurus, name);
        if (corpus.isEmpty()) {
            log.debug("Aucun corpus n'est trouvé avec le nom {} dans le thésaurus id {}", name, idThesaurus);
            return null;
        }
        return corpus.get();
    }

    public boolean updateCorpusLink(String idThesaurus, NodeCorpus nodeCorpusForEdit, String oldName) {

        log.debug("Mise à jour du corpus name {}", oldName);
        var corpusToUpdate = corpusLinkRepository.findByIdThesaurusAndCorpusName(idThesaurus, oldName);
        if (corpusToUpdate.isPresent()) {
            log.debug("Mise à jour du corpus Name {} en {}", oldName, nodeCorpusForEdit.getCorpusName());
            corpusLinkRepository.updateCorpusName(nodeCorpusForEdit.getCorpusName(), oldName, idThesaurus);

            log.debug("Mise à jour du reste des informations du corpus");
            corpusToUpdate.get().setUriLink(nodeCorpusForEdit.getUriLink());
            corpusToUpdate.get().setUriCount(nodeCorpusForEdit.getUriCount());
            corpusToUpdate.get().setOnlyUriLink(nodeCorpusForEdit.isOnlyUriLink());
            corpusToUpdate.get().setOmekaS(nodeCorpusForEdit.isOmekaS());
            corpusToUpdate.get().setActive(nodeCorpusForEdit.isActive());
            corpusLinkRepository.save(corpusToUpdate.get());
            return true;
        }
        return false;
    }

    public boolean saveCorpusLink(String idThesaurus, NodeCorpus corpusLink) {

        var corpusFind = getCorpusByNameAndThesaurus(idThesaurus, corpusLink.getCorpusName());
        if (corpusFind != null) {
            return false;
        }
        log.debug("Enregistrement du corpus dans la base de données");
        corpusLinkRepository.save(CorpusLink.builder()
                .idThesaurus(idThesaurus)
                .corpusName(corpusLink.getCorpusName())
                .active(corpusLink.isActive())
                .omekaS(corpusLink.isOmekaS())
                .onlyUriLink(corpusLink.isOnlyUriLink())
                .uriLink(corpusLink.getUriLink())
                .uriCount(corpusLink.getUriCount())
                .build());
        return true;
    }

    public void deleteCorpusLinkByThesaurusAndName(String idThesaurus, String corpusName) {

        log.debug("Suppression de corpus {} situé dans le thésaurus id {}", corpusName, idThesaurus);
        corpusLinkRepository.deleteCorpusLinkByIdThesaurusAndCorpusName(idThesaurus, corpusName);
    }

}
