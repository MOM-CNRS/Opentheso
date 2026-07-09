package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshSupport;
import lombok.RequiredArgsConstructor;

/**
 * @deprecated Conservé pour l'UI legacy v1 ; la consultation V2 utilise {@link fr.cnrs.opentheso.v2.shared.session.SessionConceptTreeRefreshSupport}.
 */
@Deprecated
@RequiredArgsConstructor
public class LegacyConceptTreeRefreshSupport implements ConceptTreeRefreshSupport {

    private final Tree tree;

    @Override
    public void refreshConceptTree() {
        try {
            tree.loadConceptTree();
        } catch (Exception ignored) {
            // l'arbre sera rechargé au prochain accès
        }
    }
}
