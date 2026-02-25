package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.utils.DateUtils;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;

import jakarta.faces.context.FacesContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@AllArgsConstructor
public class RestoreThesaurusService {

    private final GroupService groupService;
    private final ArkService arkService;
    private final ConceptService conceptService;
    private final RelationService relationService;
    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;
    private final TermService termService;
    private final ConceptRepository conceptRepository;
    private final ThesaurusRepository thesaurusRepository;


    @Transactional
    public int correctDisplayTopTerm(String idThesaurus) {
        int modified = thesaurusRepository.resetTopConceptsWithRelations(idThesaurus);
        log.info("Top concepts corrigés: {}", modified);
        return modified;
    }

    @Transactional
    public void reorganizing(String idThesaurus) {

        if(StringUtils.isEmpty(idThesaurus)) return;

        // nettoyage des null et d'espaces
        if (!thesaurusService.cleaningThesaurus(idThesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des espaces et des null");
            return;
        }

        // permet de détecter les concepts TT erronés : si le concept n'a pas de BT, alors, il est forcement TopTerme (en ignorant les candidats)
        if(!reorganizingTopTermInThesaurus(idThesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la correction des TT");
            return;
        }

        // complète le thésaurus par les relations qui manquent NT ou BT
        if(!reorganizingThesaurus(idThesaurus)){
            MessageUtils.showErrorMessage("Erreur pendant la correction des NT BT");
            return;
        }

        // permet de detecter si le concept à un BT et il est aussi TopTerm, c'est incohérent, il faut alors supprimer le status TopTerm
        if(!removeTopTermForConceptWithBT(idThesaurus)){
            MessageUtils.showErrorMessage("Erreur pendant la suppression des BT pour les topTermes");
            return;
        }

        // permet de supprimer les relations en boucle (100 -> BT -> 100) ou  (100 -> NT -> 100)ou (100 -> RT -> 100)
        if(!removeSameRelations(idThesaurus)){
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations en boucle");
        }
    }

    @Transactional
    public boolean reorganizingTopTermInThesaurus(String idThesaurus) {

        // récupération des TopTerms en tenant compte des éventuelles erreurs donc par déduction
        var listIds = relationService.getListIdOfTopTermForRepair(idThesaurus);
        for (String idConcept : listIds) {
            conceptService.setTopConceptTag(true, idConcept, idThesaurus);
        }
        return true;
    }

    public boolean removeTopTermForConceptWithBT(String idThesaurus){
        // récupération de tous les Id TT du thésaurus
        var tabIdTT = conceptService.getConceptByThesaurusAndTopConceptAndStatusNotLike(idThesaurus, true, "CA");
        for (var concept : tabIdTT) {
            if (relationService.isConceptHaveRelationBT(concept.getIdConcept(), idThesaurus)) {
                conceptService.setTopConceptTag(false, concept.getIdConcept(), idThesaurus);
            }
        }
        return true;
    }

    public boolean removeSameRelations(String idTheso) {
        if(!removeSameRelations("BT", idTheso))
            return false;
        if(!removeSameRelations("NT", idTheso))
            return false;
        return removeSameRelations("RT", idTheso);
    }

    private boolean removeSameRelations(String role, String idThesaurus) {

        // récupération des relations en Loop
        var tabRelations = relationService.getListLoopRelations(role, idThesaurus);
        if (!tabRelations.isEmpty()) {
            for (HierarchicalRelationship relation : tabRelations) {
                relationService.deleteThisRelation(relation.getIdConcept1(), idThesaurus, role, relation.getIdConcept2());
            }
        }
        return true;
    }

    /**
     * Fonction qui permet de restructurer le thésaurus en ajoutant les NT et les BT qui manquent
     * elle permet aussi de sortir les termes orphelins si nécessaire
     */
    @Transactional
    public boolean reorganizingThesaurus(String idThesaurus) {

        // récupération de tous les Id concepts du thésaurus
        var tabIdConcept = conceptService.getAllIdConceptOfThesaurus(idThesaurus);

        for (String idConcept : tabIdConcept) {
            var idBT = relationService.getListIdBT(idConcept, idThesaurus);
            var idConcept1WhereIsNT = relationService.getListIdWhichHaveNt(idConcept, idThesaurus);
            if (idBT.isEmpty() && idConcept1WhereIsNT.isEmpty()) {
                if (!conceptService.isTopConcept(idConcept, idThesaurus)) {
                    // le concept est orphelin
                    conceptService.setTopConceptTag(true, idConcept, idThesaurus);
                }
            } else {
                if (!(new HashSet<>(idBT).containsAll(idConcept1WhereIsNT))) {
                    //alors il manque des BT
                    ArrayList<String> BTmiss = new ArrayList<>(idConcept1WhereIsNT);
                    BTmiss.removeAll(idBT);
                    //on ajoute la différence
                    for (String miss : BTmiss) {
                        relationService.addHierarchicalRelation(idConcept, idThesaurus, "BT", miss);
                    }
                }
                if (!(new HashSet<>(idConcept1WhereIsNT).containsAll(idBT))) {
                    //il manque des NT pour certain idBT
                    ArrayList<String> NTmiss = new ArrayList<>(idBT);
                    NTmiss.removeAll(idConcept1WhereIsNT);
                    //on jaoute la différence
                    for (String miss : NTmiss) {
                        relationService.addHierarchicalRelation(miss, idThesaurus, "NT", idConcept);
                    }
                }
            }
        }
        return true;
    }

    public void generateSitemap(String idThesaurus) {

        if(StringUtils.isEmpty(idThesaurus)) return;

        var fileName =  idThesaurus + ".xml";
        var conceptIds = conceptService.getAllIdConceptOfThesaurus(idThesaurus);

        try {
            File file = new File(new URI(Objects.requireNonNull(this.getClass().getResource("/")).toString()) + fileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            var writeFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writeFile.write(getHeader());
            writeFile.write(getDatas(conceptIds, idThesaurus));
            writeFile.write("</urlset>");
            writeFile.close();
            MessageUtils.showInformationMessage("L'export du siteMap a réussi, nom du fichier " + fileName);
        } catch (Exception e) {
            MessageUtils.showErrorMessage("L'export du siteMap a échoué");
        }
    }

    public int generateArkFromConceptId(String idThesaurus, String prefix, String naan, boolean overwrite) {

        int count = 0;
        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if(preference == null || StringUtils.isEmpty(preference.getNaanArkLocal())) {
            MessageUtils.showErrorMessage("Pas de paramètres !! ");
            return count;
        }

        var allConcepts = conceptService.getAllIdConceptOfThesaurus(idThesaurus);
        for (String conceptId : allConcepts) {
            var concept = conceptService.getConcept(conceptId, idThesaurus);
            if(overwrite || StringUtils.isEmpty(concept.getIdArk())) {
                arkService.updateArkIdOfConcept(conceptId, idThesaurus, naan + "/" + prefix + conceptId);
                count++;
            }
        }

        return count;
    }

    private String getDatas(List<String> conceptIds, String idThesaurus) {

        var date = new DateUtils().getDate();
        var stringBuilder = new StringBuilder();
        for (String conceptId : conceptIds) {
            stringBuilder.append(getLine(getUri(conceptId, idThesaurus), date));
        }
        return stringBuilder.toString();
    }

    private String getUri(String idConcept, String idTheso){
        return  getPath() + "/?idc=" + idConcept + "&amp;idt=" + idTheso;
    }

    private String getHeader(){
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """;
    }

    private String getLine(String url, String date){
        String line = "  <url>\n";
        line = line + "    <loc>" + url + "</loc>\n";
        line = line + "    <lastmod>" + date + "</lastmod>\n";
        line = line + "  </url>\n";
        return line;
    }

    private String getPath(){
        if(FacesContext.getCurrentInstance() == null) {
            return null;
        }
        String path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    public void reorganizeConceptsAndCollections(String idThesaurus) {

        // supprimer le concept qui ont une relation vers un groupe vide
        // liste des concepts de la table concept-group-concept qui ont une relation vers un groupe qui n'existe plus
        if(!groupService.deleteConceptsWithEmptyRelation(idThesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations vides");
        }
        if(!groupService.deleteConceptsHavingRelationShipWithDeletedGroup(idThesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations interdites");
        }
        if(!groupService.deleteConceptsHavingRelationShipWithDeletedConcept(idThesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations interdites");
        }
    }

    public void deleteLoopRelations(String idThesaurus, String idConcept) {

        var listIds = conceptService.getIdsOfBranchWithoutLoop(idConcept, idThesaurus);
        for (String id : listIds) {
            var nodeRelation = relationService.getLoopRelation(idThesaurus, id);
            if(nodeRelation!= null) {
                relationService.deleteThisRelation(nodeRelation.getIdConcept2(), idThesaurus, "BT", nodeRelation.getIdConcept1());
                relationService.deleteThisRelation(nodeRelation.getIdConcept1(), idThesaurus, "NT", nodeRelation.getIdConcept2());
            }
        }
    }

    public void switchRolesFromTermToConcept(String idThesaurus, String workLanguage) {

        var allConcepts = conceptService.getAllIdConceptOfThesaurus(idThesaurus);
        for (String idConcept : allConcepts) {
            var concept = conceptService.getConcept(idConcept, idThesaurus);
            var preferredTerm = termService.getPreferenceTermByThesaurusAndConcept(idThesaurus, idConcept);

            if (preferredTerm != null) {
                var term = termService.getTermByIdAndThesaurusAndLang(preferredTerm.getIdTerm(), idThesaurus, workLanguage);
                if(concept.getCreator() != null &&  concept.getCreator() > 0) {
                    if (term != null && term.getCreator() != null && term.getCreator() > 0) {
                        concept.setContributor(term.getCreator());
                    }
                }
                if(concept.getContributor() != null &&  concept.getContributor() > 0) {
                    if (term != null && term.getContributor() != null && term.getContributor() > 0) {
                        concept.setContributor(term.getContributor());
                    }
                }
                conceptService.updateConcept(concept);
            }
        }
    }

    public int generateArkLacal(String idThesaurus, boolean overwriteLocalArk) {

        var count = 0;
        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if(preference == null) {
            MessageUtils.showErrorMessage("Pas de paramètres !! ");
            return count;
        }

        var allConcepts = conceptService.getAllIdConceptOfThesaurus(idThesaurus);
        for (String idConcept : allConcepts) {
            var concept = conceptService.getConcept(idConcept, idThesaurus);
            if(overwriteLocalArk || StringUtils.isEmpty(concept.getIdArk())) {
                var idArk = ToolsHelper.getNewId(preference.getSizeIdArkLocal(), preference.isUppercaseForArk(), true);
                var urlArk = preference.getNaanArkLocal() + "/" + preference.getPrefixArkLocal() + idArk;
                conceptRepository.setIdArk(urlArk, new Date(), idConcept, idThesaurus);
                count++;
            }
        }
        return count;
    }
}
