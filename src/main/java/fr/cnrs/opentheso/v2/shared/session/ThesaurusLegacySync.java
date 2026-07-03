package fr.cnrs.opentheso.v2.shared.session;

import java.util.Optional;

/**
 * Synchronisation avec la session legacy du thésaurus sélectionné (pont unique hors package v2.ui).
 */
public interface ThesaurusLegacySync {

    void applyThesaurusId(String thesaurusId);

    void applyThesaurusId(String thesaurusId, String language);

    Optional<String> readSelectedThesaurusId();

    Optional<String> readSelectedLanguage();

    void clearSelection();
}
