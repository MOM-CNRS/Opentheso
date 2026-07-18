package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.GroupArkLookupResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupArkPublicService {

    private final ConceptGroupRepository conceptGroupRepository;

    public GroupArkLookupResponse resolveGroupByArk(String naan, String arkId) {
        String fullArkId = naan + "/" + arkId;
        String thesaurusId = conceptGroupRepository.findThesaurusIdByArkId(fullArkId);
        if (StringUtils.isBlank(thesaurusId)) {
            throw new PublicResourceNotFoundException("Aucun groupe trouvé pour l'identifiant ARK : " + fullArkId);
        }
        String groupId = conceptGroupRepository.findAllByIdThesaurusAndIdArk(thesaurusId, fullArkId)
                .map(fr.cnrs.opentheso.entites.ConceptGroup::getIdGroup)
                .orElse(null);
        if (StringUtils.isBlank(groupId)) {
            throw new PublicResourceNotFoundException("Aucun groupe trouvé pour l'identifiant ARK : " + fullArkId);
        }
        return new GroupArkLookupResponse(thesaurusId, groupId);
    }
}
