package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import jakarta.faces.context.FacesContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionPdfUriResolver {

    private final ConceptRepository conceptRepository;

    public String getUriForConcept(Preferences preferences, String thesaurusId, String conceptId, String idArk, String idHandle) {
        if (thesaurusId == null || preferences == null) {
            return "";
        }

        if (preferences.isOriginalUriIsArk() && StringUtils.isNotEmpty(idArk)) {
            return preferences.getOriginalUri() + "/" + idArk;
        }

        if (preferences.isOriginalUriIsHandle() && StringUtils.isNotEmpty(idHandle)) {
            return "https://hdl.handle.net/" + idHandle;
        }

        if (StringUtils.isNotEmpty(preferences.getOriginalUri())) {
            return preferences.getOriginalUri() + "/?idc=" + conceptId + "&idt=" + thesaurusId;
        }

        return resolveBasePath(preferences) + "/?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    public String getIdArk(Preferences preferences, String thesaurusId, String conceptId) {
        if (preferences == null || !preferences.isOriginalUriIsArk()) {
            return null;
        }
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .map(Concept::getIdArk)
                .orElse(null);
    }

    private String resolveBasePath(Preferences preferences) {
        if (FacesContext.getCurrentInstance() == null) {
            if (StringUtils.isNotEmpty(preferences.getOriginalUri())) {
                return StringUtils.removeEnd(preferences.getOriginalUri(), "/");
            }
            return StringUtils.removeEnd(StringUtils.defaultString(preferences.getCheminSite()), "/");
        }
        var externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getRequestHeaderMap().get("origin")
                + externalContext.getRequestContextPath();
    }
}
