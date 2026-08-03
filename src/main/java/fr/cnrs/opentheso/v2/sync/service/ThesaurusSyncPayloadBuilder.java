package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThesaurusSyncPayloadBuilder {

    private final ConceptFullReadService conceptFullReadService;

    @Transactional(readOnly = true)
    public Optional<SyncConceptPayload> build(String thesaurusId, String conceptId, String lang) {
        return conceptFullReadService
                .loadFullConcept(thesaurusId, conceptId, lang, 0, true)
                .map(this::fromSnapshot);
    }

    public SyncConceptPayload fromSnapshot(ConceptFullSnapshot snapshot) {
        SyncConceptPayload.Builder builder = SyncConceptPayload.builder()
                .identifier(snapshot.getIdentifier())
                .permanentId(snapshot.getPermanentId())
                .notation(snapshot.getNotation());

        if (snapshot.getPrefLabel() != null) {
            builder.prefLabel(snapshot.getPrefLabel().lang(), snapshot.getPrefLabel().value());
        }
        addPrefTranslations(builder, snapshot);
        addAltLabels(builder, snapshot.getAltLabels());
        addAltLabels(builder, snapshot.getAltLabelTraduction());
        addNotes(builder, snapshot.getNotes(), "note");
        addNotes(builder, snapshot.getDefinitions(), "definition");
        addNotes(builder, snapshot.getScopeNotes(), "scopeNote");
        return builder.build();
    }

    private void addPrefTranslations(SyncConceptPayload.Builder builder, ConceptFullSnapshot snapshot) {
        if (CollectionUtils.isEmpty(snapshot.getPrefLabelsTraduction())) {
            return;
        }
        for (ConceptTermLabel label : snapshot.getPrefLabelsTraduction()) {
            builder.prefLabel(label.lang(), label.value());
        }
    }

    private void addAltLabels(SyncConceptPayload.Builder builder, java.util.List<ConceptTermLabel> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        for (ConceptTermLabel label : labels) {
            builder.altLabel(label.lang(), label.value());
        }
    }

    private void addNotes(
            SyncConceptPayload.Builder builder,
            java.util.List<ConceptSnapshotNote> notes,
            String type
    ) {
        if (CollectionUtils.isEmpty(notes)) {
            return;
        }
        for (ConceptSnapshotNote note : notes) {
            if (StringUtils.isBlank(note.value())) {
                continue;
            }
            switch (type) {
                case "definition" -> builder.definition(note.lang(), note.value());
                case "scopeNote" -> builder.scopeNote(note.lang(), note.value());
                default -> builder.note(note.lang(), note.value());
            }
        }
    }
}
