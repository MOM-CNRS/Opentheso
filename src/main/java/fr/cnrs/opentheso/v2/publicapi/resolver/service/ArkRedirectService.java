package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArkRedirectService {

    private enum Kind {
        CONCEPT, GROUP, THESAURUS
    }

    private final ConceptRepository conceptRepository;
    private final ThesaurusRepository thesaurusRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;

    public Optional<String> buildRedirectUrl(String naan, String arkId) {
        if (StringUtils.isBlank(arkId)) {
            return Optional.empty();
        }
        String fullArkId = naan + "/" + arkId;

        String thesaurusId = conceptRepository.findIdThesaurusListByArkId(fullArkId);
        Kind kind = Kind.CONCEPT;

        if (StringUtils.isBlank(thesaurusId)) {
            thesaurusId = thesaurusRepository.findIdThesaurusByArkId(fullArkId).orElse(null);
            kind = Kind.THESAURUS;
        }
        if (StringUtils.isBlank(thesaurusId)) {
            thesaurusId = conceptGroupRepository.findThesaurusIdByArkId(fullArkId);
            kind = Kind.GROUP;
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return Optional.empty();
        }

        var preferences = conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId).orElse(null);
        if (preferences == null) {
            return Optional.empty();
        }

        String resolvedThesaurusId = thesaurusId;
        return switch (kind) {
            case CONCEPT -> conceptRepository.findConceptIdByArkIgnoreCase(fullArkId, resolvedThesaurusId)
                    .map(idConcept -> preferences.getCheminSite() + "?idc=" + idConcept + "&idt=" + resolvedThesaurusId);
            case GROUP -> conceptGroupRepository.findAllByIdThesaurusAndIdArk(resolvedThesaurusId, fullArkId)
                    .map(group -> preferences.getCheminSite() + "?idg=" + group.getIdGroup() + "&idt=" + resolvedThesaurusId);
            case THESAURUS -> Optional.of(preferences.getCheminSite() + "?idt=" + resolvedThesaurusId);
        };
    }
}
