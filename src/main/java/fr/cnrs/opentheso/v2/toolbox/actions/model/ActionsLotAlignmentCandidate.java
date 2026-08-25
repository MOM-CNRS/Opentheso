package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

/**
 * Alignement CSV validé, prêt à être appliqué (import ou suppression).
 */
public record ActionsLotAlignmentCandidate(
        int line,
        String localId,
        String conceptId,
        String uri,
        String source,
        int alignmentTypeId
) implements Serializable {
}
