package fr.cnrs.opentheso.v2.concept.policy;

import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Droit de consultation d'un thésaurus (invité / membre / public).
 * Aligné sur le filtrage catalogue et le legacy {@code SelectedTheso#preRenderView}.
 */
@Component
@RequiredArgsConstructor
public class ThesaurusConsultationAccessPolicy {

    private final ThesaurusRepository thesaurusRepository;

    /**
     * @param thesaurusId identifiant demandé
     * @param listedInUserCatalog {@code true} si le thésaurus apparaît dans le catalogue
     *                            filtré (public, ou privé accessible au compte connecté)
     */
    public boolean canConsult(String thesaurusId, boolean listedInUserCatalog) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        if (listedInUserCatalog) {
            return true;
        }
        Boolean isPrivate = thesaurusRepository.isPrivateThesaurus(thesaurusId);
        if (isPrivate == null) {
            return false;
        }
        // Public hors catalogue (ex. filtre projet) : lecture autorisée.
        // Privé hors catalogue : invité ou non-membre → refusé.
        return !Boolean.TRUE.equals(isPrivate);
    }

    public boolean isPrivateThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId)
                && Boolean.TRUE.equals(thesaurusRepository.isPrivateThesaurus(thesaurusId));
    }
}
