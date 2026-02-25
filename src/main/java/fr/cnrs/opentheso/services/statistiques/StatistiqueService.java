package fr.cnrs.opentheso.services.statistiques;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.ConceptGroupProjection;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptStatusRepository;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


@Data
@Service
@RequiredArgsConstructor
public class StatistiqueService {

    private final NoteService noteService;
    private final GroupService groupService;
    private final ConceptService conceptService;
    private final ThesaurusService thesaurusService;
    private final ConceptStatusRepository conceptStatusRepository;
    private final AlignmentService alignmentService;
    private final ConceptRepository conceptRepository;


    public List<GenericStatistiqueData> searchAllCollectionsByThesaurus(String idThesaurus, String idLang) {

        List<GenericStatistiqueData> result = new ArrayList<>();
        
        //var listGroup = groupService.getListConceptGroup(idThesaurus, idLang);

        var listGroup = new ArrayList<>(
                groupService.getListConceptGroup(idThesaurus, idLang)
        );

        listGroup.sort(
                Comparator.comparing(NodeGroup::getLexicalValue, String.CASE_INSENSITIVE_ORDER)
        );

        listGroup.forEach(group -> {

            var noteNbr = noteService.getNbrNoteByGroup(group.getConceptGroup().getIdGroup(), idThesaurus, idLang);
            var conceptNbr = conceptStatusRepository.countConceptsInGroup(idThesaurus, group.getConceptGroup().getIdGroup());
            var traductionOfGroupNbr = conceptStatusRepository.countNonPreferredTermsByLangAndGroup(idThesaurus,
                    group.getConceptGroup().getIdGroup(), idLang);
            var WikidataAlignNbr = getNbAlignWikidata(idThesaurus, group.getConceptGroup().getIdGroup());
            var totalAlignmentNbr = getAlignementsSize(idThesaurus, group.getConceptGroup().getIdGroup()).size();
            var termesNonTraduitsNbr = conceptNbr - traductionOfGroupNbr;
            
            result.add(GenericStatistiqueData.builder()
                    .idCollection(group.getConceptGroup().getIdGroup())
                    .collection(group.getLexicalValue())
                    .notesNbr(noteNbr)
                    .synonymesNbr(traductionOfGroupNbr)
                    .conceptsNbr(conceptNbr)
                    .termesNonTraduitsNbr(conceptNbr - traductionOfGroupNbr)
                    .wikidataAlignNbr(WikidataAlignNbr)
                    .totalAlignment(totalAlignmentNbr)
                    .termesNonTraduitsNbr(termesNonTraduitsNbr)
                    .build());
        });

        var conceptNbr = conceptStatusRepository.countConceptsWithoutGroup(idThesaurus);
        result.add(GenericStatistiqueData.builder()
                .collection("Sans collection")
                .conceptsNbr(conceptNbr)
                .notesNbr(noteService.getNbrNoteSansGroup(idThesaurus, idLang))
                .synonymesNbr(conceptStatusRepository.countNonPreferredTermsNotInGroup(idThesaurus, idLang))
                .termesNonTraduitsNbr(conceptNbr - conceptStatusRepository.countConceptsWithoutGroupByLangAndThesaurus(idThesaurus, idLang))
                .wikidataAlignNbr(getNbAlignWikidata(idThesaurus, null))
                .totalAlignment(getAlignementsSize(idThesaurus, null).size())
                .build());

        return result;
    }

    public int getNbAlignWikidata(String thesaurusId, String groupId) {
        return getAlignementsSize(thesaurusId, groupId).stream()
                .filter(element -> StringUtils.isNotEmpty(element.getUriTarget()) && element.getUriTarget().contains("wikidata.org"))
                .toList()
                .size();
    }

    private List<Alignement> getAlignementsSize(String idThesaurus, String idGroup) {

        var alignements = StringUtils.isEmpty(idGroup)
                ? alignmentService.findAlignementsNotInConceptGroup(idThesaurus)
                : alignmentService.findAlignementsByGroupAndThesaurus(idGroup, idThesaurus);

        return CollectionUtils.isNotEmpty(alignements) ? alignements : List.of();
    }

    public List<ConceptStatisticData> searchAllConceptsByThesaurus(String idTheso, String idLang, Date dateDebut, Date dateFin,
                                                                   String collectionId, String nbrResultat) {

        if (ObjectUtils.isEmpty(dateDebut) && !ObjectUtils.isEmpty(dateFin)) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Il faut préciser la date de fin !");
            return List.of();
        }

        if (!ObjectUtils.isEmpty(dateDebut) && !ObjectUtils.isEmpty(dateFin) && dateDebut.after(dateFin)) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La date de début est plus récente que la date de fin !");
            return List.of();
        }

        if (!ObjectUtils.isEmpty(dateDebut) && ObjectUtils.isEmpty(dateFin)) {
            dateFin = new Date();
        }

        int limit;
        try {
            limit = Integer.parseInt(nbrResultat);
        } catch (Exception e) {
            limit = 100;
        }

        if(ObjectUtils.isEmpty(dateDebut) || ObjectUtils.isEmpty(dateFin)) {

            var result = StringUtils.isEmpty(collectionId)
                    ? conceptStatusRepository.findRecentConceptsByLangAndThesaurus(idTheso, idLang, limit)
                    : conceptStatusRepository.findConceptsByGroupAndLang(idTheso, idLang, collectionId, limit);
            return conceptStatisticDataMapper(result);
        } else {
            var result = (StringUtils.isEmpty(collectionId))
                ? conceptStatusRepository.findConceptsModifiedBetween(idTheso, idLang, dateDebut, dateFin, limit)
                : conceptStatusRepository.findConceptsByGroupLangDate(idTheso, idLang, collectionId, dateDebut, dateFin, limit);
            return conceptStatisticDataMapper(result);
        }
    }

    public int countValidConceptsByThesaurus(String idThesaurus) {
        return conceptStatusRepository.countValidConceptsByThesaurus(idThesaurus);
    }

    public List<Concept> findAllByIdThesaurusAndStatus(String idThesaurus, String status) {
        return conceptService.getConceptByThesaurusAndStatus(idThesaurus, status);
    }

    private List<ConceptStatisticData> conceptStatisticDataMapper(List<ConceptGroupProjection> conceptGroupProjectionsList) {

        var dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        return CollectionUtils.isEmpty(conceptGroupProjectionsList)
                ? List.of()
                : conceptGroupProjectionsList.stream()
                    .map(element -> ConceptStatisticData.builder()
                        .idConcept(element.getIdConcept())
                        .dateCreation(ObjectUtils.isEmpty(element.getCreated()) ? null : dataFormat.format(element.getCreated()))
                        .dateModification(ObjectUtils.isEmpty(element.getModified()) ? null : dataFormat.format(element.getModified()))
                        .label(element.getLexicalValue())
                        .utilisateur(element.getUsername())
                        .type("skos:prefLabel")
                        .build())
                    .toList();
    }

    public List<DomaineDto> getListGroupes(String idThesaurus, String idLangue) {
        var thesaurus = thesaurusService.getThesaurusById(idThesaurus);
        if (thesaurus != null) {
            return groupService.findAllByIdThesaurusAndLang(thesaurus.getIdThesaurus(), idLangue).stream()
                    .map(element ->
                            DomaineDto.builder()
                                    .id(element.getIdGroup())
                                    .name(element.getLexicalValue())
                                    .build())
                    .toList();
        }
        return List.of();
    }

    public int getNbCpt(String idThesaurus) {

        return conceptRepository.countConcepts(idThesaurus);
    }

    public int getNbCandidate(String idThesaurus) {

        return conceptRepository.countCandidate(idThesaurus);
    }

    public int getNbOfDeprecatedConcepts(String idThesaurus) {

        return conceptRepository.countConceptDeprecated(idThesaurus);
    }

}
