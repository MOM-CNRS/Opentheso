package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class SessionConceptTreeRefreshSupport implements ConceptTreeRefreshSupport {

    private final ConceptTreeRefreshState conceptTreeRefreshState;
    private final ObjectProvider<ConceptNavigationSupport> conceptNavigationSupportProvider;

    @Override
    public void refreshConceptTree() {
        conceptTreeRefreshState.requestRefresh();
        try {
            ConceptNavigationSupport navigation = conceptNavigationSupportProvider.getIfAvailable();
            if (navigation != null) {
                navigation.invalidateConceptTree();
                conceptTreeRefreshState.consumeRefresh();
            }
        } catch (Exception ignored) {
            // l'arbre sera invalidé au prochain accès à la consultation V2
        }
    }
}
