package fr.cnrs.opentheso.v2.shared.session;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implémentation autonome : l'état du thésaurus est porté par {@code ThesaurusContext}.
 */
@Component
@Primary
public class SessionThesaurusSync implements ThesaurusLegacySync {

    @Override
    public void applyThesaurusId(String thesaurusId) {
        applyThesaurusId(thesaurusId, null);
    }

    @Override
    public void applyThesaurusId(String thesaurusId, String language) {
        // État géré par ThesaurusContext.
    }

    @Override
    public Optional<String> readSelectedThesaurusId() {
        return Optional.empty();
    }

    @Override
    public Optional<String> readSelectedLanguage() {
        return Optional.empty();
    }

    @Override
    public void clearSelection() {
        // État géré par ThesaurusContext.
    }
}
