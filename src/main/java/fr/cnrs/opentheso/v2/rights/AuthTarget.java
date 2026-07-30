package fr.cnrs.opentheso.v2.rights;

import org.apache.commons.lang3.StringUtils;

/** Cible optionnelle d'un contrôle de droit. */
public record AuthTarget(Integer projectId, String thesaurusId) {

    public static AuthTarget none() {
        return new AuthTarget(null, null);
    }

    public static AuthTarget project(int projectId) {
        return new AuthTarget(projectId, null);
    }

    public static AuthTarget thesaurus(String thesaurusId) {
        return new AuthTarget(null, thesaurusId);
    }

    public boolean hasProject() {
        return projectId != null;
    }

    public boolean hasThesaurus() {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
