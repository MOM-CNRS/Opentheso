package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.entites.ConceptGroupLabelHistorique;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.concept.NodeAutoCompletion;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.group.NodeGroupLabel;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;



@Slf4j
@Service
@AllArgsConstructor
public class GroupService {

    private final HandleService handleHelper;
    private final PreferenceService preferenceService;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;
    private final ConceptGroupHistoriqueRepository conceptGroupHistoriqueRepository;

    private final RelationGroupService relationGroupService;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;


    public void insertGroup(String idGroup, String idThesaurus, String idArk, String typeCode, String notation, Date created, Date modified) {

        log.debug("Ajout d'un nouveau group (MT, domaine etc..) avec le libellé dans le cas d'un import avec idGroup existant");
        var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (conceptGroup.isEmpty()) {

            conceptGroupRepository.save(ConceptGroup.builder()
                    .id(conceptGroupRepository.getNextConceptGroupSequence().intValue())
                    .idGroup(idGroup.toLowerCase())
                    .idArk(StringUtils.isEmpty(idArk) ? "" : idArk)
                    .idThesaurus(idThesaurus)
                    .idTypeCode(typeCode)
                    .notation(notation)
                    .idHandle("")
                    .idDoi("")
                    .created(created == null ? new Date() : created)
                    .modified(modified == null ? new Date() : modified)
                    .build());
        } else {
            conceptGroup.get().setIdArk(StringUtils.isEmpty(idArk) ? "" : idArk);
            conceptGroup.get().setIdTypeCode(typeCode);
            conceptGroup.get().setNotation(notation);
            conceptGroup.get().setModified(new Date());
            conceptGroupRepository.save(conceptGroup.get());
        }
    }

    public boolean addConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {

        log.debug("Enregistrement d'un nouveau group de concept");
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
        return true;
    }

    public NodeGroupLabel getNodeGroupLabel(String idConceptGroup, String idThesaurus) {

        log.debug("Recherche des détails du group {}", idConceptGroup);

        var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idConceptGroup, idThesaurus);
        if (conceptGroup.isEmpty()) {
            log.error("Aucun group n'est présent avec l'id {}", idConceptGroup);
            return null;
        }

        log.debug("Création du model du group {}", idConceptGroup);
        return NodeGroupLabel.builder()
                .idGroup(idConceptGroup)
                .idThesaurus(idThesaurus)
                .idArk(conceptGroup.get().getIdArk())
                .idHandle(conceptGroup.get().getIdHandle())
                .notation(conceptGroup.get().getNotation())
                .created(conceptGroup.get().getCreated())
                .modified(conceptGroup.get().getModified())
                .nodeGroupTraductionses(getAllGroupTraduction(idConceptGroup, idThesaurus))
                .build();
    }

    public void saveUserGroupThesaurus(UserGroupThesaurus userGroupThesaurus) {

        log.debug("Enregistrement du nouveau user group Thesaurus");
        userGroupThesaurusRepository.save(userGroupThesaurus);
    }

    public void deleteRelationConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {

        log.debug("Suppression de la relation entre le concept id {} et le group id {}", idConcept, idGroup);
        conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(idGroup, idConcept, idThesaurus);
    }

    public boolean removeAllConceptsFromThisGroup(String idGroup, String idThesaurus) {

        log.debug("Suppression du lien entre le group {} et les conceptes rattachés à ce dernier", idGroup);
        conceptGroupConceptRepository.deleteAllByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        return true;
    }

    public boolean deleteConceptsHavingRelationShipWithDeletedConcept(String idThesaurus) {
        try {
            var orphanLinks = conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts(idThesaurus);

            for (Object[] row : orphanLinks) {
                String idGroup = (String) row[0];
                String idConcept = (String) row[1];
                conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(idGroup, idConcept, idThesaurus);
            }

            return true;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: " + idThesaurus, e);
            return false;
        }
    }

    public boolean deleteConceptsHavingRelationShipWithDeletedGroup(String idThesaurus) {

        try {
            log.debug("Suppression des concepts qui ont une relation vers une collection supprimée");
            var orphanLinks = conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts(idThesaurus);
            for (var row : orphanLinks) {
                String idGroup = (String) row[0];
                removeAllConceptsFromThisGroup(idGroup, idThesaurus);
            }
            return true;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: " + idThesaurus, e);
            return false;
        }
    }

    public void setGroupVisibility(String idGroup, String idThesaurus, boolean isPrivate) {

        log.debug("Changement de la visibilité du group {} en {}", idGroup, isPrivate);
        conceptGroupRepository.updateVisibility(idGroup, idThesaurus, isPrivate);
    }

    public List<NodeIdValue> searchGroup(String idThesaurus, String idLang, String text) {

        log.debug("Recherche la liste des groups pour l'autocomplétion avec la valeur {} dans le thésaurus {} (langue : {})",
                text, idThesaurus, idLang);

        text = fr.cnrs.opentheso.utils.StringUtils.convertString(text);
        var results = conceptGroupLabelRepository.searchGroups(idThesaurus, idLang, text);
        if (CollectionUtils.isEmpty(results)) {
            log.debug("Aucun group trouvé avec la valeur {} dans le thésaurus {} (langue : {})", text, idThesaurus, idLang);
            return List.of();
        }

        log.debug("{} groups trouvés pour l'autocomplétion avec la valeur {} dans le thésaurus {} (langue : {})",
                results.size(), text, idThesaurus, idLang);
        return results.stream()
                .map(element -> NodeIdValue.builder()
                        .id((String) element[0])
                        .value((String) element[1])
                        .build())
                .toList();
    }

    public List<NodeAutoCompletion> getAutoCompletionGroup(String idThesaurus, String idLang, String text) {

        log.debug("Recherche la liste des groups pour l'autocomplétion avec la valeur {} dans le thésaurus {} (langue : {})",
                text, idThesaurus, idLang);
        var cleanedText = fr.cnrs.opentheso.utils.StringUtils.convertString(text);
        var results = conceptGroupLabelRepository.getGroupAutoCompletions(idThesaurus, idLang, cleanedText);
        if (CollectionUtils.isEmpty(results)) {
            log.debug("Aucun group trouvé avec la valeur {} dans le thésaurus {} (langue : {})", text, idThesaurus, idLang);
            return List.of();
        }

        log.debug("{} groups trouvés pour l'autocomplétion avec la valeur {} dans le thésaurus {} (langue : {})",
                results.size(), text, idThesaurus, idLang);
        return results.stream()
                .map(element -> NodeAutoCompletion.builder()
                        .idConcept("")
                        .prefLabel("")
                        .groupLexicalValue((String) element[1])
                        .idGroup((String) element[0])
                        .build())
                .toList();
    }

    public List<NodeGroupTraductions> getAllGroupTraduction(String idGroup, String idThesaurus) {

        var groupLabels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroup(idThesaurus, idGroup);
        if (CollectionUtils.isEmpty(groupLabels)) {
            log.debug("Aucune traduction n'existe pour le group {}", idGroup);
            return List.of();
        }

        log.debug("{} traductions est trouvées pour le group {}", groupLabels.size(), idGroup);
        return groupLabels.stream().map(element ->
            NodeGroupTraductions.builder()
                    .idLang(element.getLang())
                    .title(element.getLexicalValue())
                    .created(element.getCreated())
                    .modified(element.getModified())
                    .build()
        ).toList();
    }

    private void generateArkIdLocal(String idThesaurus, String idGroup) {

        log.debug("Génération de l'id Ark local pour le group");

        var preferenceThesaurus = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferenceThesaurus == null) {
            log.error("Aucune préférence n'est trouvé pour le thésaurus {}", idThesaurus);
            return;
        }

        if (!preferenceThesaurus.isUseArkLocal()) {
            log.error("le flag 'useArkLocal' est à false pour le thésaurus {}", idThesaurus);
            return;
        }

        var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (conceptGroup.isEmpty()) {
            log.error("Aucun group n'est présent avec l'id {}", idGroup);
            return;
        }

        var idArk = conceptGroup.get().getIdArk();
        if(StringUtils.isEmpty(idArk)) {
            log.debug("Aucune valeur n'est présente pour 'idArk'");
            idArk = ToolsHelper.getNewId(preferenceThesaurus.getSizeIdArkLocal(), preferenceThesaurus.isUppercaseForArk(), true);
            idArk = preferenceThesaurus.getNaanArkLocal() + "/" + preferenceThesaurus.getPrefixArkLocal() + idArk;
        }
        updateArkIdOfGroup(idGroup, idThesaurus, idArk);
    }

    @Transactional
    public String addGroup(NodeGroup nodeConceptGroup, int idUser) {

        log.debug("Enregistrement d'un nouveau group !");

        var newId = getNewIdGroup();
        var idSequenceConcept = newId.intValue();
        var idGroup = "g" + newId;
        log.debug("Le nouveau id group est {}", idGroup);

        if (nodeConceptGroup.getConceptGroup().getNotation() == null) {
            nodeConceptGroup.getConceptGroup().setNotation("");
        }

        log.debug("Insertion du nouveau group dans la base de données");
        conceptGroupRepository.save(ConceptGroup.builder()
                .id(idSequenceConcept)
                .idGroup(idGroup)
                .idArk("")
                .idThesaurus(nodeConceptGroup.getConceptGroup().getIdThesaurus())
                .idTypeCode(nodeConceptGroup.getConceptGroup().getIdTypeCode())
                .notation(nodeConceptGroup.getConceptGroup().getNotation())
                .idHandle("")
                .idDoi("")
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Ajout de la traduction du group {}", idGroup);
        conceptGroupLabelRepository.save(ConceptGroupLabel.builder()
                .idGroup(idGroup)
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(nodeConceptGroup.getLexicalValue()))
                .created(new Date())
                .modified(new Date())
                .lang(nodeConceptGroup.getIdLang())
                .idThesaurus(nodeConceptGroup.getConceptGroup().getIdThesaurus())
                .build());

        addGroupTraductionHistoriqueRollBack(idGroup, nodeConceptGroup.getConceptGroup().getIdThesaurus(),
                nodeConceptGroup.getIdLang(), nodeConceptGroup.getLexicalValue(), idUser);

        var thesaurusPreferences = preferenceService.getThesaurusPreferences(nodeConceptGroup.getConceptGroup().getIdThesaurus());
        if (thesaurusPreferences != null) {
            if (thesaurusPreferences.isUseArk()) {

                log.debug("Création de l'identifiant Ark pour le group {}", idGroup);
                var nodeMetaData = new NodeMetaData();
                nodeMetaData.setCreator("");
                nodeMetaData.setTitle(nodeConceptGroup.getLexicalValue());
                nodeMetaData.setDcElementsList(new ArrayList<>());
                addIdArk__(idGroup, nodeConceptGroup.getConceptGroup().getIdThesaurus(), nodeMetaData, thesaurusPreferences);
            }

            log.debug("Création de l'identifiant Handle pour le group {}", idGroup);
            if (thesaurusPreferences.isUseHandle()) {
                addIdHandle(idGroup, nodeConceptGroup.getConceptGroup().getIdThesaurus(), thesaurusPreferences);
            }
        }

        return idGroup;
    }

    private void addIdArk__(String idGroup, String idThesaurus, NodeMetaData nodeMetaData, Preferences thesaurusPreferences) {

        log.debug("Début de la création de l'identifiant Ark pour le group {}", idGroup);
        var arkHelper2 = new ArkHelper2(thesaurusPreferences);
        if (!arkHelper2.login()) {
            return;
        }

        var privateUri = "?idg=" + idGroup.toLowerCase() + "&idt=" + idThesaurus;
        if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
            return;
        }

        var idHandle = StringUtils.isEmpty(arkHelper2.getIdHandle()) ? "" : arkHelper2.getIdHandle();
        if (!updateArkIdOfGroup(idGroup, idThesaurus, arkHelper2.getIdArk())) {
            return;
        }

        if (thesaurusPreferences.isGenerateHandle()) {
            updateHandleIdOfGroup(idGroup.toLowerCase(), idThesaurus, idHandle);
        }
    }

    private boolean updateArkIdOfGroup(String idGroup, String idThesaurus, String idArk) {

        log.debug("Mise jour de l'id Ark pour le concept Group {}", idGroup);
        var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (conceptGroup.isEmpty()) {
            log.debug("Aucun group n'est trouvé avec id {} et thésaurus id {}", idGroup, idThesaurus);
            return false;
        }

        conceptGroup.get().setIdArk(idArk);
        conceptGroupRepository.save(conceptGroup.get());
        log.debug("Fin de la mise jour d'id Ark pour le concept Group {}", idGroup);
        return true;
    }

    private void addIdHandle(String idGroup, String idThesaurus, Preferences preferences) {

        var privateUri = "?idg=" + idGroup.toLowerCase() + "&idt=" + idThesaurus;
        var idHandle = handleHelper.addIdHandle(privateUri, preferences);

        if (StringUtils.isNotEmpty(idHandle)) {
            updateHandleIdOfGroup(idGroup, idThesaurus, idHandle);
        }
    }

    private boolean updateHandleIdOfGroup(String idGroup, String idThesaurus, String idHandle) {

        log.debug("Mise à jour du handle pour le group {}", idGroup);
        var group = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (group.isEmpty()) {
            log.debug("Aucun group n'est trouvé avec id {}", idGroup);
            return false;
        }

        group.get().setIdHandle(idHandle);
        log.debug("Mise à jour du handle du group {} est terminée", idGroup);
        return true;
    }

    public Long getNewIdGroup() {
        Long idGroup;
        do {
            idGroup = conceptGroupRepository.getNextConceptGroupSequence();
        } while (conceptGroupRepository.countByIdGroupIgnoreCase("d" + idGroup) > 0);
        return idGroup;
    }

    public List<String> getListIdGroupOfConcept(String idThesaurus, String idConcept) {

        log.debug("Recherche de la liste des groups présents dans le thésaurus {} et concept {}", idThesaurus, idConcept);
        var groups = conceptGroupConceptRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        if (CollectionUtils.isEmpty(groups)) {
            log.debug("Aucun group n'est trouvé pour l'id thésaurus {} and id concept {}", idThesaurus, idConcept);
        }

        log.debug("{} groups trouvés", groups.size());
        return groups.stream().map(ConceptGroupConcept::getIdGroup).toList();
    }

    public List<NodeGroup> getListConceptGroup(String idThesaurus, String idLang) {

        log.debug("Recherche de la liste des groups présents dans thésaurus {}", idThesaurus);
        var idGroups = getGroupsByThesaurus(idThesaurus);

        return idGroups.stream().map(idGroup -> {
            log.debug("Recherche des informations sur le group {}", idGroup);
            return getThisConceptGroup(idGroup, idThesaurus, idLang);
        }).toList();
    }

    public List<String> getGroupsByThesaurus(String idThesaurus) {

      //  var groups = conceptGroupRepository.findAllByIdThesaurus(idThesaurus);
        var groups = conceptGroupRepository.findAllByIdThesaurus(idThesaurus);

        if (CollectionUtils.isEmpty(groups)) {
            log.debug("Aucun group n'est disponible dans le thésaurus id {}", idThesaurus);
            return new ArrayList<>();
        }

        log.debug("{} groups trouvés pour le thésaurus id {}", groups.size(), idThesaurus);
        return groups.stream().map(ConceptGroup::getIdGroup).toList();
    }

    public boolean deleteGroupTraduction(String idGroup, String idThesaurus, String idLang) {

        log.debug("Suppression de la traduction du group id {}", idGroup);
        var conceptGroupLabel = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(idThesaurus, idGroup, idLang);
        if (conceptGroupLabel.isEmpty()) {
            log.error("Aucun concept group n'est trouvé avec l'id Group {}", idGroup);
            return false;
        }

        conceptGroupLabelRepository.deleteAllByIdGroupAndIdThesaurusAndLang(idGroup, idThesaurus, idLang);
        log.debug("Fin de la suppression de la traduction du group id {}", idGroup);
        return true;
    }

    @Transactional
    public boolean renameGroup(String label, String idLang, String idGroup, String idThesaurus, int idUser) {

        log.debug("Mise à jour du nom du group id {}", idGroup);
        var conceptGroupLabel = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(idThesaurus, idGroup, idLang);
        if (conceptGroupLabel.isEmpty()) {
            log.error("Aucun concept group n'est trouvé avec l'id Group {}", idGroup);
            return false;
        }

        log.debug("Mise à jour du nom du group dans la base de données");
        conceptGroupLabel.get(0).setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label));
        conceptGroupLabel.get(0).setModified(new Date());
        conceptGroupLabelRepository.save(conceptGroupLabel.get(0));

        addGroupTraductionHistoriqueRollBack(idGroup, idThesaurus, idLang, label, idUser);
        updateModifiedDate(idGroup, idThesaurus);

        log.debug("Fin de la mise à jour du nom du group id {}", idGroup);
        return true;
    }

    public void updateModifiedDate(String idGroup, String idThesaurus) {

        log.debug("Mise à jour de la date de modification du group id {}", idGroup);
        conceptGroupRepository.updateModifiedDate(idGroup.toLowerCase(), idThesaurus);
    }

    public List<NodeGroup> getListGroupOfConcept(String idThesaurus, String idConcept, String idLang) {

        var groups = conceptGroupConceptRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        if (groups.isEmpty()) {
            log.debug("Aucun group n'est trouvé pour le concept id {}", idConcept);
        }

        log.debug("{} groups trouvés pour le concept id {}", groups.size(), idConcept);
        return groups.stream()
                .map(element -> getThisConceptGroup(element.getIdGroup(), idThesaurus, idLang))
                .toList();

    }

    public NodeGroup getThisConceptGroup(String idGroup, String idThesaurus, String idLang) {

        log.debug("Rechercher du group id {}", idGroup);
        var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (conceptGroup.isEmpty()) {
            log.debug("Aucun group n'est trouvé pour l'id Group {}", idGroup);
            return null;
        }

        log.debug("Rechercher du label du group id {} avec la langue {}", idGroup, idLang);
        var conceptGroupDetail = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(idThesaurus,
                conceptGroup.get().getIdGroup(), idLang);

        return NodeGroup.builder()
                .groupPrivate(conceptGroup.get().isPrivate())
                .conceptGroup(conceptGroup.get())
                .lexicalValue(CollectionUtils.isNotEmpty(conceptGroupDetail) ? conceptGroupDetail.get(0).getLexicalValue() : "")
                .idLang(idLang)
                .created(CollectionUtils.isNotEmpty(conceptGroupDetail) ? conceptGroupDetail.get(0).getCreated() : null)
                .modified(CollectionUtils.isNotEmpty(conceptGroupDetail) ? conceptGroupDetail.get(0).getModified() : null)
                .build();
    }

    public boolean isIdGroupExiste(String idGroup, String idThesaurus) {

        log.debug("Vérification de l'existance d'un group {} rattaché au thésaurus id {}", idGroup, idThesaurus);
        var group = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        return group.isPresent();
    }

    public boolean isHaveTraduction(String idGroup, String idThesaurus, String idLang) {

        log.debug("Vérification de l'existance du group {} avec la langue {}", idGroup, idLang);
        var conceptGroup = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(idGroup, idThesaurus, idLang);
        return CollectionUtils.isNotEmpty(conceptGroup);
    }

    private void addGroupTraductionHistoriqueRollBack(String idGroup, String idThesaurus, String idLang, String value, int idUser) {

        log.debug("Enregistrement d'une nouvelle historique pour la table ConceptGroupLabel");
        conceptGroupLabelHistoriqueRepository.save(ConceptGroupLabelHistorique.builder()
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(value))
                .lang(idLang)
                .idThesaurus(idThesaurus)
                .idGroup(idGroup.toLowerCase())
                .idUser(idUser)
                .modified(new Date())
                .build());
    }

    public boolean isMoveToDescending(String idGroup, String toIdGroup, String idThesaurus){

        log.debug("Vérification si le group à déplacer ({}) est vers un descendant ({}) -> c'est interdit", idGroup, toIdGroup);
        var idGroupDescending = getAllGroupDescending(idGroup, idThesaurus);
        return idGroupDescending.contains(toIdGroup);
    }

    /**
     * Récupération des IdGroup fils par récurcivité
     */
    public List<String> getAllGroupDescending(String idGroup, String idThesaurus) {

        log.debug("Rechercher de la liste des Id Groups descendant du group id {} ", idGroup);
        return getDescendingId_(idGroup, idThesaurus, new ArrayList<>());
    }

    private List<String> getDescendingId_(String idGroup, String idThesaurus, List<String> allIdGroup) {

        allIdGroup.add(idGroup);
        var listIds = relationGroupService.getListGroupChildIdOfGroup(idGroup, idThesaurus);

        if (CollectionUtils.isEmpty(listIds)) {
            return allIdGroup;
        }

        for (String idGroupFils : listIds) {
            getDescendingId_(idGroupFils, idThesaurus, allIdGroup);
        }
        return allIdGroup;
    }

    public boolean setNotationOfGroup(String notation, String idGroup, String idThesaurus) {

        log.debug("Modification de la notation du group id {} ", idGroup);
        var group = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (group.isEmpty()) {
            log.debug("Aucun group n'existe dans la base avec l'id {}", idGroup);
            return false;
        }

        group.get().setNotation(notation);
        conceptGroupRepository.save(group.get());
        log.debug("Mise à jour de la notation terminé pour le group {}", idGroup);
        return true;
    }

    public List<NodeGroupTraductions> getGroupTraduction(String idGroup, String idThesaurus, String idLang) {

        log.debug("Recherche de toutes les traductions du group id {} (sans celle qui est en cours)", idGroup);
        var conceptGroupLabel = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLangNot(idThesaurus, idGroup, idLang);
        if (CollectionUtils.isEmpty(conceptGroupLabel)) {
            log.debug("Aucune traduction n'est trouvée pour le group {}", idGroup);
            return List.of();
        }

        log.debug("{} traductions trouvées pour le group {}", conceptGroupLabel.size(), idGroup);
        return conceptGroupLabel.stream()
                .map(element -> NodeGroupTraductions.builder()
                        .idLang(element.getLang())
                        .title(element.getLexicalValue())
                        .build()
                ).toList();
    }

    @Transactional
    public boolean addIdArkGroup(String idThesaurus, String idGroup, String labelGroup) {

        log.debug("Génération de l'id Ark pour le group");

        var preferenceThesaurus = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferenceThesaurus == null) {
            log.error("Aucune préférence n'est trouvé pour le thésaurus {}", idThesaurus);
            return false;
        }

        if (preferenceThesaurus.isUseArkLocal()) {
            log.debug("L'indicateur 'useArkLocal' pour le group {} est activé", idGroup);
            generateArkIdLocal(idThesaurus, idGroup);
            return true;
        }

        ArkHelper2 arkHelper2 = new ArkHelper2(preferenceThesaurus);
        if (!arkHelper2.login()) {
            log.debug("Erreur de connexion au serveur Ark !");
            return false;
        }

        if (preferenceThesaurus.isUseArk()) {

            var privateUri = "?idg=" + idGroup.toLowerCase() + "&idt=" + idThesaurus;
            var nodeMetaData = NodeMetaData.builder()
                    .title(labelGroup)
                    .creator("")
                    .dcElementsList(new ArrayList<>())
                    .build();

            if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
                throw new RuntimeException("Erreur lors de l'ajout d'un ark du group " + idGroup);
            }

            if (!updateArkIdOfGroup(idGroup, idThesaurus, arkHelper2.getIdArk())) {
                throw new RuntimeException("Erreur lors la mise à jour du l'id Ark du group " + idGroup);
            }

            if (preferenceThesaurus.isGenerateHandle()) {
                if (!updateHandleIdOfGroup(idGroup, idThesaurus, arkHelper2.getIdHandle())) {
                    throw new RuntimeException("Erreur lors la mise à jour du handle du group " + idGroup);
                }
            }
        }

        updateModifiedDate(idGroup, idThesaurus);
        return true;
    }

    public NodeUri getThisGroupIds(String idGroup, String idThesaurus) {

        log.debug("Recherche des détails du group {}", idGroup);
        var groups = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);
        if (groups.isEmpty()) {
            log.debug("Aucun group n'est trouvé avec l'id {}", idGroup);
            return null;
        }

        return NodeUri.builder()
                .idConcept(groups.get().getIdGroup())
                .idArk(groups.get().getIdArk())
                .idHandle(groups.get().getIdHandle())
                .idDoi(groups.get().getIdDoi())
                .build();
    }

    public boolean isGroupHaveConcepts(String idGroup, String idThesaurus) {

        log.debug("Vérifier si le group id {} a des concepts", idGroup);
        var concepts = conceptGroupConceptRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus);

        log.debug("Le group {} contient des concepts : {}", idGroup, !CollectionUtils.isEmpty(concepts));
        return !CollectionUtils.isEmpty(concepts);
    }

    public boolean isDomainExist(String title, String idThesaurus, String idLang) {

        log.debug("Vérifier si le group {} existe avec la langue {}", title, idLang);

        title = fr.cnrs.opentheso.utils.StringUtils.convertString(title);
        var group = conceptGroupLabelRepository.findByLexicalValueLikeAndLangAndIdThesaurus(title, idLang, idThesaurus);
        log.debug("Le group {} existe avec la langue {} ? {}", title, idLang, group.isPresent());
        return group.isPresent();
    }

    public List<NodeGroup> getListRootConceptGroup(String idThesaurus, String idLang, boolean isSortByNotation, boolean isPrivate) {

        log.debug("Rechercher la liste des domaines de premier niveau (MT, G, C, T)");
        List<NodeGroup> nodeConceptGroupList = new ArrayList<>();

        var tabIdConceptGroup = getListIdOfRootGroup(idThesaurus, isPrivate);
        for (String idGroup : tabIdConceptGroup) {

            var nodeConceptGroup = getThisConceptGroup(idGroup, idThesaurus, idLang);
            if (nodeConceptGroup == null) {
                return null;
            }

            if (relationGroupService.isHaveSubGroup(idThesaurus, idGroup)) {
                nodeConceptGroup.setHaveChildren(true);
            }

            if (isGroupHaveConcepts(idGroup, idThesaurus)) {
                nodeConceptGroup.setHaveChildren(true);
            }

            if (StringUtils.isEmpty(nodeConceptGroup.getLexicalValue())) {
                nodeConceptGroup.setLexicalValue("__" + idGroup);
            }

            nodeConceptGroupList.add(nodeConceptGroup);
        }
        if (!isSortByNotation) {
            Collections.sort(nodeConceptGroupList);
        }
        return nodeConceptGroupList;
    }

    public List<String> getListIdOfRootGroup(String idThesaurus, boolean isPrivate) {

        log.debug("Recherche des groupes racines pour le thésaurus : {} (filtrage public = {})", idThesaurus, isPrivate);
        var results = isPrivate
                ? conceptGroupRepository.findRootGroupsPublicOnly(idThesaurus)
                : conceptGroupRepository.findRootGroups(idThesaurus);

        if (CollectionUtils.isEmpty(results)) {
            log.debug("Aucun group n'est trouvé dans le thésaurus id {}", idThesaurus);
            return Collections.emptyList();
        }

        log.debug("Nombre de groupes racines trouvés : {}", results.size());
        return results;
    }

    public List<NodeGroup> getListChildsOfGroup(String idConceptGroup, String idThesaurus, String idLang, boolean isSortByNotation) {

        log.debug("Recherche des sous-groupes d'un Group (type G/C/MT/T)");
        var lisIdGroups = relationGroupService.getListGroupChildIdOfGroup(idConceptGroup, idThesaurus);
        if (CollectionUtils.isEmpty(lisIdGroups)) {
            log.debug("Aucun group n'est rattaché au group {}", idConceptGroup);
            return null;
        }

        List<NodeGroup> nodeGroups = new ArrayList<>();
        for (String idGroup : lisIdGroups) {
            var nodeConceptGroup = getThisConceptGroup(idGroup, idThesaurus, idLang);
            if (nodeConceptGroup == null) {
                log.debug("Aucun détail n'est trouvé pour le group id {}", idGroup);
                continue;
            }
            nodeConceptGroup.setHaveChildren(relationGroupService.isHaveSubGroup(idThesaurus, idGroup));
            nodeConceptGroup.setHaveChildren(isGroupHaveConcepts(idGroup, idThesaurus));

            if (StringUtils.isEmpty(nodeConceptGroup.getLexicalValue())) {
                nodeConceptGroup.setLexicalValue("__" + idGroup);
            }
            nodeGroups.add(nodeConceptGroup);
        }
        if (!isSortByNotation) {
            Collections.sort(nodeGroups);
        }

        return nodeGroups;
    }

    public String getIdGroupFromHandleId(String idHandle) {

        log.debug("Recherche de l'id group à partir de l'id handle {}", idHandle);
        var group = conceptGroupRepository.findByIdHandle(idHandle);
        if (group.isEmpty()) {
            log.debug("Aucun group n'est trouvé avec l'id Handle {}", idHandle);
            return null;
        }

        log.debug("L'id Group est {} contenant l'id handle {}", group.get().getIdGroup(), idHandle);
        return group.get().getIdGroup();
    }

    public String getIdGroupFromArkId(String idArk, String idThesaurus) {

        log.debug("Recherche de l'id group à partir de l'id ark {} et l'id thésaurus {}", idThesaurus, idThesaurus);
        var group = conceptGroupRepository.findAllByIdThesaurusAndIdArk(idThesaurus, idArk);
        if (group.isEmpty()) {
            log.debug("Aucun group n'est trouvé avec l'id ark {} et l'id thésaurus {}", idThesaurus, idThesaurus);
            return null;
        }

        log.debug("L'id Group est {} contenant l'id ark {} et id thésaurus {}", group.get().getIdGroup(), idArk, idThesaurus);
        return group.get().getIdGroup();
    }

    public void addGroupTraduction(fr.cnrs.opentheso.models.group.ConceptGroupLabel conceptGroupLabel, int idUser) {

        log.debug("Ajouter une nouvelle traduction à un group domaine");
        conceptGroupLabel.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(conceptGroupLabel.getLexicalValue()));
        conceptGroupLabelRepository.save(ConceptGroupLabel.builder()
                .lexicalValue(conceptGroupLabel.getLexicalValue())
                .lang(conceptGroupLabel.getLang())
                .idThesaurus(conceptGroupLabel.getIdthesaurus())
                .idGroup(conceptGroupLabel.getIdgroup().toLowerCase() )
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Ajouter une traduction de domaine en historique");
        conceptGroupLabelHistoriqueRepository.save(ConceptGroupLabelHistorique.builder()
                .lexicalValue(conceptGroupLabel.getLexicalValue())
                .lang(conceptGroupLabel.getLang())
                .idThesaurus(conceptGroupLabel.getIdthesaurus())
                .idGroup(conceptGroupLabel.getIdgroup().toLowerCase())
                .idUser(idUser)
                .modified(new Date())
                .build());
    }

    public boolean deleteConceptsWithEmptyRelation(String idThesaurus) {

        log.debug("Supprimer l'appartenance des concepts avec des relations vide vers une collection");
        conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdGroup(idThesaurus, "");
        return true;
    }

    public void addGroupTraduction(String idGroup, String idThesaurus, String idLang, String value) {

        log.debug("Ajouter un libellé pour un group (MT, domaine, etc..)");
        value = fr.cnrs.opentheso.utils.StringUtils.convertString(value);
        conceptGroupLabelRepository.save(ConceptGroupLabel.builder()
                .lexicalValue(value)
                .created(new Date())
                .modified(new Date())
                .lang(idLang)
                .idThesaurus(idThesaurus)
                .idGroup(idGroup.toLowerCase())
                .build());
    }

    public boolean isNotationExist(String notation, String idThesaurus) {

        log.debug("Rechercher si la notation {} existe dans le thésaurus id {}", notation, idThesaurus);
        var groups = conceptGroupRepository.findByIdThesaurusAndNotation(idThesaurus, notation);
        log.debug("La notation {} existe existe dans le thésaurus id {} : {}", notation, idThesaurus, CollectionUtils.isNotEmpty(groups));
        return CollectionUtils.isNotEmpty(groups);
    }

    public void deleteConceptGroupRollBack(String idGroup, String idThesaurus) {

        log.debug("Suppression du groupe id {} et ses traductions !", idGroup);

        log.debug("Suppression de toutes les traductions du groupe id {}", idGroup);
        conceptGroupLabelRepository.deleteByIdThesaurusAndIdGroup(idThesaurus, idGroup);

        log.debug("Suppression du groupe id {}", idGroup);
        conceptGroupRepository.deleteByIdThesaurusAndIdGroup(idThesaurus, idGroup);

        log.debug("Suppression de toutes les relations avec le groupe id {}", idGroup);
        relationGroupService.deleteRelationGroup(idThesaurus, idGroup);
    }

    public String getIdThesaurusFromArkId(String arkId) {

        log.debug("Recherche de l'idThesaurus pour l'arkId : {}", arkId);
        var idThesaurus = conceptGroupRepository.findThesaurusIdByArkId(arkId);
        log.debug("Thesaurus trouvé pour l'arkId {} : {}", arkId, idThesaurus);
        return idThesaurus;
    }

    public List<NodeUri> getListGroupOfConceptArk(String idThesaurus, String idConcept) {

        log.debug("Recherche des groupes liés au concept : {} dans le thésaurus {}", idConcept, idThesaurus);
        var result = conceptGroupRepository.findGroupsOfConcept(idThesaurus, idConcept);
        if (CollectionUtils.isEmpty(result)) {
            log.debug("Aucun group n'est lié au concept : {} dans le thésaurus {}", idConcept, idThesaurus);
            return Collections.emptyList();
        }

        log.debug("{} groups liés au concept : {} dans le thésaurus {}", result.size(), idConcept, idThesaurus);
        return result.stream().map(row ->
            NodeUri.builder()
                    .idConcept((String) row[0])
                    .idArk(row[1] != null ? (String) row[1] : "")
                    .idHandle(row[2] != null ? (String) row[2] : "")
                    .idDoi(row[3] != null ? (String) row[3] : "")
                    .build()
        ).toList();
    }

    public void cleanGroup() {

        log.debug("Nettoyage des valeurs des notations");
        conceptGroupRepository.cleanNotation();

        log.debug("Nettoyage des valeurs des idTypeCode");
        conceptGroupRepository.cleanIdTypeCode();
    }

    public void deleteAllGroupsByThesaurus(String idThesaurus) {

        log.debug("Suppression de tous les groups du thésaurus {}", idThesaurus);
        conceptGroupRepository.deleteByIdThesaurus(idThesaurus);

        log.debug("Suppression de tous les traduction des groups du thésaurus {}", idThesaurus);
        conceptGroupLabelRepository.deleteByIdThesaurus(idThesaurus);

        log.debug("Suppression des relations entre les groups");
        relationGroupService.deleteRelationGroupByThesaurus(idThesaurus);
    }

    public void updateThesaurusId(String newThesaurusID, String oldThesaurusID) {

        log.debug("Mise à jour du thésaurus id pour les groups (du {} vers {})", oldThesaurusID, newThesaurusID);
        conceptGroupRepository.updateThesaurusId(newThesaurusID, oldThesaurusID);
        conceptGroupLabelRepository.updateThesaurusId(newThesaurusID, oldThesaurusID);
        conceptGroupLabelHistoriqueRepository.updateThesaurusId(newThesaurusID, oldThesaurusID);
        conceptGroupConceptRepository.updateThesaurusId(newThesaurusID, oldThesaurusID);
        relationGroupService.updateIdThesaurus(newThesaurusID, oldThesaurusID);
        conceptGroupHistoriqueRepository.updateThesaurusId(newThesaurusID, oldThesaurusID);
    }

    public void deleteAllGroupOfConcept(String idConcept, String idThesaurus) {

        log.debug("Supprimer tous les groupes du concept id {}", idConcept);
        conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdConcept(idThesaurus, idConcept);
    }

    public List<NodeIdValue> getDomaineCandidatByConceptAndThesaurusAndLang(String idconcept, String idThesaurus, String lang) {

        var nodeGroups = getListGroupOfConcept(idThesaurus, idconcept, lang);

        List<NodeIdValue> nodeIdValues = new ArrayList<>();
        for (NodeGroup nodeGroup : nodeGroups) {
            if(nodeGroup == null) continue;
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setValue(nodeGroup.getLexicalValue());
            nodeIdValue.setId(nodeGroup.getConceptGroup().getIdGroup());
            nodeIdValues.add(nodeIdValue);
        }
        return nodeIdValues;
    }

    public void addNewDomaine(String idGroup, String idThesaurus, String idConcept) {

        log.debug("Création d'un nouveau group");
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .build());
    }

    public List<ConceptGroupLabel> findAllByIdThesaurusAndLang(String idThesaurus, String idLang) {

        var groups = conceptGroupLabelRepository.findAllByIdThesaurusAndLang(idThesaurus, idLang);
        if (groups.isEmpty()) {
            log.error("Aucun group trouvé dans le thésaurus id {} avec la langue {}", idThesaurus, idLang);
            return List.of();
        }
        return groups;
    }

}
