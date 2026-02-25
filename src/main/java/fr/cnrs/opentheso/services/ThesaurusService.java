package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.thesaurus.NodeThesaurus;
import fr.cnrs.opentheso.repositories.ExternalImageRepository;
import fr.cnrs.opentheso.repositories.ExternalResourceRepository;
import fr.cnrs.opentheso.repositories.GraphViewExportedConceptBranchRepository;
import fr.cnrs.opentheso.repositories.LanguageIso639Repository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.RoutineMailRepository;
import fr.cnrs.opentheso.repositories.ThesaurusAlignementSourceRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusHomePageRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserRoleOnlyOnRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@AllArgsConstructor
public class ThesaurusService {

    private final ThesaurusRepository thesaurusRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final LanguageIso639Repository languageIso639Repository;
    private final ThesaurusHomePageRepository thesaurusHomePageRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final ExternalResourceRepository externalResourceRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final NodeLabelRepository nodeLabelRepository;
    private final RoutineMailRepository routineMailRepository;
    private final ThesaurusAlignementSourceRepository thesaurusAlignementRepository;
    private final GraphViewExportedConceptBranchRepository graphViewExportedConceptBranchRepository;
    private final ThesaurusAlignementSourceRepository thesaurusAlignementSourceRepository;

    private final TermService termService;
    private final GroupService groupService;
    private final ConceptService conceptService;
    private final CandidatService candidatService;
    private final GpsService gpsService;
    private final AlignmentService alignmentService;
    private final PropositionService propositionService;
    private final RelationService relationService;
    private final ImageService imageService;
    private final PreferenceService preferenceService;
    private final NoteService noteService;
    private final UserRoleOnlyOnRepository userRoleOnlyOnRepository;
    private final ExternalImageRepository externalImageRepository;

    public List<ThesaurusDcTerm> getDcTermsOfThesaurus(String idThesaurus) {
        return thesaurusDcTermRepository.findAllByIdThesaurus(idThesaurus);
    }

    public Thesaurus getThesaurusById(String idThesaurus) {

        log.debug("Rechercher un thésaurus à partir de son id {}", idThesaurus);
        var thesaurus = thesaurusRepository.findById(idThesaurus);
        if (thesaurus.isEmpty()) {
            log.error("Le thésaurus avec id {} n'existe pas dans la base de données", idThesaurus);
            return null;
        }
        return thesaurus.get();
    }

    public List<String> getIsoLanguagesOfThesaurus(String idThesaurus) {

        log.debug("Recherche des langues ISO pour le thésaurus '{}'", idThesaurus);
        var langues = thesaurusLabelRepository.findDistinctLangByIdThesaurus(idThesaurus);

        if (CollectionUtils.isEmpty(langues)) {
            log.warn("Aucune langue trouvée pour le thésaurus '{}'", idThesaurus);
            return Collections.emptyList();
        }

        log.debug("{} langue(s) trouvée(s) pour le thésaurus '{}': {}", langues.size(), idThesaurus, langues);
        return langues;
    }

    public List<NodeIdValue> getThesaurusOfProject(int idGroup, String idLang) {

        log.debug("Recherche de la liste des thésaurus définis dans le projet {} et avec la langue {}", idGroup, idLang);
        var thesaurusList = userGroupThesaurusRepository.findAllByIdGroup(idGroup);
        if (CollectionUtils.isEmpty(thesaurusList)) {
            log.debug("Aucun thésaurus n'est trouvé dans le group {}", idGroup);
            return List.of();
        }

        log.debug("{} thésaurus trouvés dans le group {}", thesaurusList.size(), idGroup);
        return thesaurusList.stream()
                .map(thesaurus -> {
                    var idLangTemp = preferenceService.getWorkLanguageOfThesaurus(thesaurus.getIdThesaurus());
                    idLangTemp = StringUtils.isEmpty(idLangTemp) ? idLang : idLangTemp;

                    var thesaurusLabel = thesaurusLabelRepository.findByIdThesaurusAndLang(thesaurus.getIdThesaurus(), idLangTemp);
                    var value = thesaurusLabel.isPresent() ? thesaurusLabel.get().getTitle() : "";

                    var thesaurusDetails = thesaurusRepository.findById(thesaurus.getIdThesaurus());
                    var status = thesaurusDetails.isPresent() && thesaurusDetails.get().getIsPrivate();

                    return NodeIdValue.builder()
                            .id(thesaurus.getIdThesaurus())
                            .value(value)
                            .status(status)
                            .build();
                }
        ).toList();
    }

    public void deleteThesaurusTraduction(String idThesaurus, String idLang) {

        log.debug("Suppression de la traduction en {} du thésaurus {}", idLang, idThesaurus);
        idThesaurus = fr.cnrs.opentheso.utils.StringUtils.convertString(idThesaurus);
        thesaurusLabelRepository.deleteByIdThesaurusAndLang(idThesaurus, idLang);
        log.debug("Suppression terminé de la traduction en {} du thésaurus {}", idLang, idThesaurus);
    }

    public boolean isLanguageExistOfThesaurus(String idThesaurus, String idLang) {

        log.debug("Vérification si la traduction en langue {} est présente pour le thésaurus {}", idLang, idThesaurus);
        var thesaurus = thesaurusLabelRepository.findByIdThesaurusAndLang(idThesaurus, idLang);
        log.debug("La traduction en langue {} est présente pour le thésaurus {} : {}", idLang, idThesaurus, thesaurus.isPresent());
        return thesaurus.isEmpty();
    }

    public List<String> getAllUsedLanguagesOfThesaurus(String idThesaurus) {

        log.debug("Rechercher les langues utilisées par les concepts du thésaurus {}", idThesaurus);
        var terms = termService.searchDistinctLangInThesaurus(idThesaurus);
        if (CollectionUtils.isEmpty(terms)) {
            log.debug("Aucune langue n'est utilisé dans le thésaurus {}", idThesaurus);
            return List.of();
        }

        log.debug("{} langues utilisées dans le thésaurus {}", terms.size(), idThesaurus);
        return terms;
    }

    public boolean isThesaurusExiste(String idThesaurus) {

        log.debug("Vérifier si le thésaurus {} existe", idThesaurus);
        var thesaurus = thesaurusRepository.findById(idThesaurus);
        log.debug("Le thésaurus id {} est trouvé ? {}", idThesaurus, thesaurus.isPresent());
        return thesaurus.isPresent();
    }

    public List<String> getAllIdOfThesaurus(boolean withPrivateThesaurus) {

        log.debug("Recherche des id des thésaurus (avec les thésaurus privées : {})", withPrivateThesaurus);
        var thesaurus = withPrivateThesaurus ? thesaurusRepository.findAllOrderByCreatedDesc() : thesaurusRepository.findAllByIsPrivateFalseOrderByCreatedDesc();

        if (CollectionUtils.isEmpty(thesaurus)) {
            log.debug("Aucun thésaurus trouvé !");
            return List.of();
        }

        log.debug("{} thésaurus trouvés", thesaurus.size());
        return thesaurus.stream().map(Thesaurus::getIdThesaurus).toList();
    }

    public boolean updateIdArkOfThesaurus(String idThesaurus, String idArk) {

        log.debug("Mise à jour de l'idArk du thésaurus id {} avec la valeur {}", idThesaurus, idArk);
        var thesaurus = thesaurusRepository.findById(idThesaurus);
        if (thesaurus.isEmpty()) {
            log.error("Le thésaurus avec id {} n'existe pas dans la base de données", idThesaurus);
            return true;
        }

        thesaurus.get().setIdArk(idArk);
        thesaurusRepository.save(thesaurus.get());
        log.debug("Mise à jour thésaurus {} avec la nouvelle valeur d'idArk {}", idThesaurus, idArk);
        return false;
    }

    public List<NodeIdValue> getAllThesaurus(boolean withPrivateThesaurus) {

        log.debug("Rechercher les thésaurus dans la langue source de chaque thésaurus");
        var tabIdThesaurus = getAllIdOfThesaurus(withPrivateThesaurus);
        return tabIdThesaurus.stream().map(idThesaurus ->
            NodeIdValue.builder()
                    .id(idThesaurus)
                    .value(getTitleOfThesaurus(idThesaurus, preferenceService.getWorkLanguageOfThesaurus(idThesaurus)))
                    .build()
        ).toList();
    }

    public String getTitleOfThesaurus(String idThesaurus, String idLang) {

        var thesaurusLabel = thesaurusLabelRepository.findByIdThesaurusAndLang(idThesaurus, idLang);
        if (thesaurusLabel.isEmpty()) {
            log.debug("Le thésaurus n'a pas de Label dans sa langue source, id {}", idThesaurus);
            return "";
        }

        log.debug("Le titre du thésaurus (id = {}) est {}", idThesaurus, thesaurusLabel.get().getTitle());
        return thesaurusLabel.get().getTitle();
    }

    public String addThesaurusRollBack() {

        log.debug("Création d'un nouveau thésaurus");
        var thesaurusSeq = thesaurusRepository.getNextThesaurusSequenceValue();
        var idThesaurus = "th" + thesaurusSeq;
        while (isThesaurusExiste(idThesaurus)) {
            idThesaurus = "th" + ++thesaurusSeq;
        }
        log.debug("Le nouveau id du thésaurus est {}", idThesaurus);

        var thesaurus = thesaurusRepository.save(Thesaurus.builder()
                .id((int)thesaurusSeq)
                .idThesaurus(idThesaurus)
                .idArk("")
                .isPrivate(true)
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Enregistrement terminé du nouveau thésaurus {}", thesaurus.getIdThesaurus());
        return thesaurus.getIdThesaurus();
    }


    public void setThesaurusVisibility(String idThesaurus, boolean isPrivateTheso) {

        log.debug("Changement de la visibilité du thésaurus id {} en {}", idThesaurus, isPrivateTheso);
        thesaurusRepository.updateVisibility(idThesaurus, isPrivateTheso);
    }

    public Boolean isPrivateThesaurus(String idThesaurus) {

        log.debug("Savoir si le thésaurus est privé id {}", idThesaurus);
        return thesaurusRepository.isPrivateThesaurus(idThesaurus);
    }

    public List<NodeLangTheso> getAllUsedLanguagesOfThesaurusNode(String idThesaurus, String idLang) {

        log.debug("Recherche des langues utilisées dans le thésaurus '{}'", idThesaurus);
        final var langue = StringUtils.isBlank(idLang) ? "fr" : idLang;

        var projections = thesaurusRepository.findAllUsedLanguagesOfThesaurus(idThesaurus);
        if (CollectionUtils.isEmpty(projections)) {
            log.debug("Aucune langue est utilisée par le thésaurus {}", idThesaurus);
            return Collections.emptyList();
        }

        log.debug("{} langue(s) utilisée(s) trouvée(s) pour le thésaurus '{}'", projections.size(), idThesaurus);
        return projections.stream()
                .map(element -> NodeLangTheso.builder()
                        .id(element.getId())
                        .code(element.getCode())
                        .codeFlag(element.getCodeFlag())
                        .labelTheso(element.getLabelTheso())
                        .value("fr".equalsIgnoreCase(langue) ? element.getFrenchName() : element.getEnglishName())
                        .build())
                .toList();
    }

    public String getIdThesaurusFromArkId(String arkId) {

        log.debug("Recherche de l'identifiant du thésaurus à partir d'Ark id {}", arkId);
        var idThesaurus = thesaurusRepository.findIdThesaurusByArkId(arkId).orElse(null);
        log.debug("L'id du thésaurus est {}", idThesaurus);
        return idThesaurus;
    }

    public NodeThesaurus getNodeThesaurus(String idThesaurus) {

        log.debug("Recherche des détails du thésaurus id {}", idThesaurus);

        log.debug("Recherche di thésaurus id {}", idThesaurus);
        var thesaurus = getThesaurusById(idThesaurus);
        if (thesaurus == null) {
            log.error("Aucun thésaurus n'est trouvé avec l'id {}", idThesaurus);
            return null;
        }

        log.debug("Recherche des langues utilisées par le thésaurus id {}", idThesaurus);
        var listLangThesaurus = getLanguagesOfThesaurus(idThesaurus);

        return NodeThesaurus.builder()
                .idThesaurus(idThesaurus)
                .idArk(thesaurus.getIdArk())
                .listThesaurusTraduction(getTraductions(listLangThesaurus, idThesaurus))
                .build();
    }

    private List<fr.cnrs.opentheso.models.thesaurus.Thesaurus> getTraductions(List<LanguageIso639> listLangThesaurus, String idThesaurus) {

        log.debug("Recherche des traductions du thésaurus id {}", idThesaurus);
        if (CollectionUtils.isEmpty(listLangThesaurus)) {
            log.debug("Aucune traduction n'est disponible avec l'id {}", idThesaurus);
            return Collections.emptyList();
        } else {
            return listLangThesaurus.stream()
                    .map(element -> getByIdAndLang(idThesaurus, element.getIso6391()))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public fr.cnrs.opentheso.models.thesaurus.Thesaurus getByIdAndLang(String idThesaurus, String idLang) {

        log.debug("Recherche du thésaurus id {} avec la langue {}", idThesaurus, idLang);
        var thesaurus = thesaurusRepository.getThesaurusByIdAndLang(idThesaurus, StringUtils.trimToEmpty(idLang));
        if (thesaurus.isEmpty()) {
            log.error("Aucun thésaurus n'existe avec l'id {} et la langue {}", idThesaurus, idLang);
            return null;
        }
        return thesaurus.get();
    }

    public List<LanguageIso639> getLanguagesOfThesaurus(String idThesaurus) {

        log.debug("Recherche des langues utilisées dans le thésaurus '{}'", idThesaurus);
        var result = languageIso639Repository.findLanguagesByThesaurusId(idThesaurus);
        if (CollectionUtils.isEmpty(result)) {
            log.warn("Aucune langue trouvée pour le thésaurus '{}'", idThesaurus);
            return Collections.emptyList();
        }

        log.debug("{} langue(s) trouvée(s) pour le thésaurus '{}'", result.size(), idThesaurus);
        return result.stream()
                .map(element ->
                    LanguageIso639.builder()
                            .iso6391(element.getIso6391())
                            .iso6392(element.getIso6392())
                            .englishName(element.getEnglishName())
                            .frenchName(element.getFrenchName())
                            .codePays(element.getCodePays())
                            .build()
                )
                .toList();
    }

    public void addThesaurusTraductionRollBack(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {

        var thesaurusToSave = addQuotes(thesaurus);
        log.debug("Création d'une nouvelle traduction pour le thésaurus {}", thesaurusToSave.getId_thesaurus());
        thesaurusLabelRepository.save(ThesaurusLabel.builder()
                .idThesaurus(thesaurusToSave.getId_thesaurus())
                .lang(thesaurusToSave.getLanguage().trim())
                .title(thesaurusToSave.getTitle())
                .contributor(thesaurusToSave.getContributor())
                .coverage(thesaurusToSave.getCoverage())
                .creator(thesaurusToSave.getCreator())
                .description(thesaurusToSave.getDescription())
                .format(thesaurusToSave.getFormat())
                .publisher(thesaurusToSave.getPublisher())
                .relation(thesaurusToSave.getRelation())
                .rights(thesaurusToSave.getRights())
                .source(thesaurusToSave.getSource())
                .subject(thesaurusToSave.getSubject())
                .type(thesaurusToSave.getType())
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .build());
    }

    public void addThesaurusTraduction(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurusToSave) {

        log.debug("Ajout d'une traduction au thésaurus {}", thesaurusToSave.getId_thesaurus());
        var thesaurus = addQuotes(thesaurusToSave);
        thesaurusLabelRepository.save(ThesaurusLabel.builder()
                .idThesaurus(thesaurus.getId_thesaurus())
                .contributor(thesaurus.getContributor())
                .coverage(thesaurus.getCoverage())
                .creator(thesaurus.getCreator())
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .description(thesaurus.getDescription())
                .format(thesaurus.getFormat())
                .lang(thesaurus.getLanguage().trim())
                .publisher(thesaurus.getPublisher())
                .relation(thesaurus.getRelation())
                .rights(thesaurus.getRights())
                .source(thesaurus.getSource())
                .subject(thesaurus.getSubject())
                .title(thesaurus.getTitle())
                .type(thesaurus.getType())
                .build());
    }

    public boolean updateThesaurus(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {

        var thesaurusToSave = addQuotes(thesaurus);

        var thesaurusLabel = thesaurusLabelRepository.findByIdThesaurusAndLang(thesaurusToSave.getId_thesaurus(), thesaurusToSave.getLanguage());
        if (thesaurusLabel.isEmpty()) {
            log.error("Aucun thésaurus trouvé avec l'id {} et langue {}", thesaurusToSave.getId_thesaurus(), thesaurusToSave.getLanguage());
            return false;
        }

        thesaurusLabel.get().setContributor(thesaurusToSave.getContributor());
        thesaurusLabel.get().setCoverage(thesaurusToSave.getCoverage());
        thesaurusLabel.get().setCreator(thesaurusToSave.getCreator());
        thesaurusLabel.get().setModified(LocalDateTime.now());
        thesaurusLabel.get().setDescription(thesaurusToSave.getDescription());
        thesaurusLabel.get().setFormat(thesaurusToSave.getFormat());
        thesaurusLabel.get().setPublisher(thesaurusToSave.getPublisher());
        thesaurusLabel.get().setRelation(thesaurusToSave.getRelation());
        thesaurusLabel.get().setRights(thesaurusToSave.getRights());
        thesaurusLabel.get().setSource(thesaurusToSave.getSource());
        thesaurusLabel.get().setSubject(thesaurusToSave.getSubject());
        thesaurusLabel.get().setTitle(thesaurusToSave.getTitle());
        thesaurusLabel.get().setType(thesaurusToSave.getType());
        thesaurusLabelRepository.save(thesaurusLabel.get());
        log.debug("Mise à jour de la traduction terminée correctement");
        return true;
    }

    private fr.cnrs.opentheso.models.thesaurus.Thesaurus addQuotes(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {

        log.debug("Ajouter des cotes pour passer des données en JDBC");
        thesaurus.setContributor(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getContributor()));
        thesaurus.setCoverage(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getCoverage()));
        thesaurus.setCreator(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getCreator()));
        thesaurus.setDescription(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getDescription()));
        thesaurus.setFormat(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getFormat()));
        thesaurus.setPublisher(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getPublisher()));
        thesaurus.setRelation(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getRelation()));
        thesaurus.setRights(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getRights()));
        thesaurus.setSource(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getSource()));
        thesaurus.setSubject(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getSubject()));
        thesaurus.setTitle(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getTitle()));
        thesaurus.setType(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getType()));
        return thesaurus;
    }

    @Transactional
    public boolean cleaningThesaurus(String idThesaurus) {

        log.debug("Nettoyer du thésaurus {} des espaces et des null", idThesaurus);
        termService.deleteTerm("", idThesaurus);
        groupService.deleteConceptGroupRollBack("", idThesaurus);
        groupService.cleanGroup();
        conceptService.cleanConcept();
        return true;
    }

    public boolean deleteThesaurus(String idThesaurus) {

        log.debug("Suppression du thésaurus id {}", idThesaurus);
        if (!isThesaurusExiste(idThesaurus)) {
            log.error("Le thésaurus id {} n'existe pas", idThesaurus);
            return false;
        }

        idThesaurus = fr.cnrs.opentheso.utils.StringUtils.convertString(idThesaurus);
        thesaurusHomePageRepository.deleteAllByIdTheso(idThesaurus);
        userGroupThesaurusRepository.deleteByIdThesaurus(idThesaurus);
        userRoleOnlyOnRepository.deleteByThesaurusIdThesaurus(idThesaurus);
        thesaurusAlignementSourceRepository.deleteAllByIdThesaurus(idThesaurus);
        thesaurusArrayRepository.deleteAllByIdThesaurus(idThesaurus);
        nodeLabelRepository.deleteAllByIdThesaurus(idThesaurus);
        thesaurusDcTermRepository.deleteAllByIdThesaurus(idThesaurus);
        thesaurusAlignementRepository.deleteAllByIdThesaurus(idThesaurus);
        graphViewExportedConceptBranchRepository.deleteAllByTopConceptThesaurusId(idThesaurus);
        routineMailRepository.deleteAllByIdThesaurus(idThesaurus);
        termService.deleteAllTermsInThesaurus(idThesaurus);
        groupService.deleteAllGroupsByThesaurus(idThesaurus);
        candidatService.deleteAllCandidatsByThesaurus(idThesaurus);
        gpsService.deleteGpsByThesaurus(idThesaurus);
        alignmentService.deleteAllAlignmentsByThesaurus(idThesaurus);
        propositionService.deleteByThesaurus(idThesaurus);
        relationService.deleteAllByThesaurus(idThesaurus);
        imageService.deleteImagesByThesaurus(idThesaurus);
        externalResourceRepository.deleteAllByIdThesaurus(idThesaurus);
        externalImageRepository.deleteAllByIdThesaurus(idThesaurus);
        noteService.deleteByThesaurus(idThesaurus);
        conceptService.deleteByThesaurus(idThesaurus);
        thesaurusRepository.deleteById(idThesaurus);
        thesaurusLabelRepository.deleteByIdThesaurus(idThesaurus);
        preferenceService.deletePreferenceThesaurus(idThesaurus);
        return true;
    }

    public boolean changeIdOfThesaurus(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Changement de l'id du thésaurus du {} vers {}", oldIdThesaurus, newIdThesaurus);
        if (isThesaurusExiste(newIdThesaurus)) {
            log.error("Un thésaurus avec l'id {} existe déjà !", newIdThesaurus);
            return false;
        }
        newIdThesaurus = fr.cnrs.opentheso.utils.StringUtils.convertString(newIdThesaurus);

        thesaurusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusLabelRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusHomePageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        userGroupThesaurusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        userRoleOnlyOnRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusAlignementSourceRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusArrayRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        nodeLabelRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusDcTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusAlignementRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        graphViewExportedConceptBranchRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        routineMailRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        termService.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        groupService.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        candidatService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        preferenceService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        gpsService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        alignmentService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        propositionService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        relationService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        imageService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        externalResourceRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        externalImageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        noteService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        conceptService.updateThesaurusId(oldIdThesaurus, newIdThesaurus);

        return true;
    }

    public void deleteDroitByThesaurus(String idThesaurusToDelete) {

        userGroupThesaurusRepository.deleteByIdThesaurus(idThesaurusToDelete);
    }
}
