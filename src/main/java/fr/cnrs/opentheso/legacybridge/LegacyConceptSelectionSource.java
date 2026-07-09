package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.v2.shared.session.ConceptSelectionSource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * @deprecated Conservé pour l'UI legacy v1 ; la consultation V2 utilise {@link fr.cnrs.opentheso.v2.shared.session.SessionConceptSelectionSource}.
 */
@Deprecated
@RequiredArgsConstructor
public class LegacyConceptSelectionSource implements ConceptSelectionSource {

    private final Tree tree;

    @Override
    public Optional<String> getSelectedConceptId() {
        return Optional.ofNullable(tree.getIdConceptSelected()).filter(StringUtils::isNotBlank);
    }
}
