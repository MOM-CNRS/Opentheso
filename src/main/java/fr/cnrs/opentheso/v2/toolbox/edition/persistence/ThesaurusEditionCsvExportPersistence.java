package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.models.NodeDeprecatedProjection;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvAlignmentRow;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvExportPersistence {

    private final ConceptRepository conceptRepository;
    private final ConceptReplacedByRepository conceptReplacedByRepository;
    private final TermRepository termRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NoteRepository noteRepository;
    private final AlignementRepository alignementRepository;

    public List<String> listConceptIds(String thesaurusId, List<String> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA").stream()
                    .map(Concept::getIdConcept)
                    .toList();
        }
        List<String> conceptIds = new ArrayList<>();
        for (String groupId : groupIds) {
            conceptIds.addAll(conceptRepository.findAllConceptIdsByGroup(thesaurusId, groupId));
        }
        return conceptIds;
    }

    public Optional<ThesaurusCsvByIdRow> loadConceptForCsvById(String conceptId, String thesaurusId, String languageCode) {
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        if (concept.isEmpty()) {
            return Optional.empty();
        }

        String prefLabel = termRepository.getLexicalValueOfConcept(conceptId, thesaurusId, languageCode).orElse("");

        List<String> altLabels = nonPreferredTermRepository
                .findNodeEMByConceptAndLang(conceptId, thesaurusId, languageCode).stream()
                .map(NodeEM::getLexicalValue)
                .toList();

        List<String> definitions = noteRepository
                .findAllByIdentifierAndIdThesaurusAndLang(conceptId, thesaurusId, languageCode).stream()
                .filter(note -> "definition".equalsIgnoreCase(note.getNoteTypeCode()))
                .map(Note::getLexicalValue)
                .toList();

        List<ThesaurusCsvAlignmentRow> alignments = alignementRepository
                .findAllAlignmentsByConceptAndThesaurus(conceptId, thesaurusId).stream()
                .map(alignment -> new ThesaurusCsvAlignmentRow(alignment.getLabel(), alignment.getUri_target()))
                .toList();

        var entity = concept.get();
        return Optional.of(new ThesaurusCsvByIdRow(
                conceptId,
                entity.getIdArk(),
                entity.getIdHandle(),
                prefLabel,
                altLabels,
                definitions,
                alignments));
    }

    public List<NodeDeprecated> listDeprecatedConcepts(String thesaurusId, String languageCode) {
        List<NodeDeprecated> deprecatedList = new ArrayList<>();
        for (NodeDeprecatedProjection projection : conceptRepository.findAllDeprecatedConcepts(thesaurusId, languageCode)) {
            NodeDeprecated node = new NodeDeprecated();
            node.setDeprecatedId(projection.getIdConcept());
            node.setDeprecatedLabel(projection.getLexicalValue());
            node.setModified(projection.getModified());
            node.setUserName(projection.getUsername());

            List<NodeIdValue> replacesValues = listReplacedBy(thesaurusId, node.getDeprecatedId(), languageCode);
            if (!replacesValues.isEmpty()) {
                node.setReplacedById(replacesValues.stream().map(NodeIdValue::getId).collect(Collectors.joining("##")));
                node.setReplacedByLabel(replacesValues.stream().map(NodeIdValue::getValue).collect(Collectors.joining("##")));
            }
            deprecatedList.add(node);
        }
        return deprecatedList;
    }

    private List<NodeIdValue> listReplacedBy(String thesaurusId, String deprecatedConceptId, String languageCode) {
        return conceptReplacedByRepository.findAllByIdConcept1AndIdThesaurus(deprecatedConceptId, thesaurusId).stream()
                .map(replacement -> {
                    String label = termRepository
                            .getLexicalValueOfConcept(replacement.getIdConcept2(), thesaurusId, languageCode)
                            .orElse("");
                    return NodeIdValue.builder()
                            .id(replacement.getIdConcept2())
                            .value(StringUtils.defaultString(label))
                            .build();
                })
                .toList();
    }
}
