package fr.cnrs.opentheso.v2.collection.model;

import java.util.List;

/**
 * Fiche collection affichée dans le panneau principal (équivalent legacy {@code groupView}).
 */
public record CollectionDetail(
        String groupId,
        String label,
        String lang,
        String typeCode,
        String typeLabel,
        String typeSkosLabel,
        int memberCount,
        String notation,
        String arkId,
        String handleId,
        String created,
        String modified,
        List<CollectionTranslationItem> translations,
        List<CollectionNoteItem> notes
) {

    public static CollectionDetail empty() {
        return new CollectionDetail(
                "", "", "", "", "", "", 0,
                "", "", "", "", "",
                List.of(), List.of()
        );
    }
}
