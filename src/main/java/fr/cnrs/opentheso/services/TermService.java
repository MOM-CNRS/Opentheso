package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.*;
import fr.cnrs.opentheso.models.NodeEMProjection;
import fr.cnrs.opentheso.models.terms.NodeTerm;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.*;
import fr.cnrs.opentheso.utils.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Data
@Slf4j
@Service
@AllArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final ConceptRepository conceptRepository;
    private final PermutedRepository permutedRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final TermHistoriqueRepository termHistoriqueRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;


    /// new methods #MR
    public boolean updatePrefLabel(String idConcept,
                                String idThesaurus,
                                String lang,
                                String label,
                                int idUser) {

        if (org.apache.commons.lang3.StringUtils.isBlank(label)) {
            throw new IllegalArgumentException("Label vide interdit");
        }
        int updatedRows = termRepository.updatePrefLabel(
                idConcept,
                idThesaurus,
                lang,
                label.trim(),
                idUser
        );
        if(updatedRows > 0) {
         /*   conceptService.updateDateOfConcept(idThesaurus,
                    idConcept,
                    idUser);

            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value("currentUser.getNodeUser().getName()")
                    .idConcept(idConcept)
                    .idThesaurus(idThesaurus)
                    .build());*/


        //    conceptRepository.updateDateOfConcept(idConcept, idThesaurus, idUser);
        }
        return updatedRows > 0;
    }



    public String addTerm(Term term, String idConcept, int idUser) {

        log.debug("Ajout d'un nouveau term {}", term.getLexicalValue());
        var idTerm = generateNextIdTerm(term);
        log.debug("Le nouveau id Term est {}", idTerm);

        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(StringUtils.convertString(term.getLexicalValue()))
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .contributor(idUser)
                .creator(idUser)
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Ajout d'une trace de création d'un nouveau term {}", term.getLexicalValue());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(termSaved.getIdTerm())
                .lexicalValue(termSaved.getLexicalValue())
                .lang(termSaved.getLang())
                .idThesaurus(termSaved.getIdThesaurus())
                .source(termSaved.getSource())
                .status(termSaved.getStatus())
                .idUser(idUser)
                .action("ADD")
                .modified(LocalDateTime.now())
                .build());

        addLinkTerm(idConcept, term.getIdThesaurus(), termSaved.getIdTerm());

        return idTerm;
    }

    private String generateNextIdTerm(Term term) {
        int idTermNum = termRepository.getMaxInternalId();
        String idTerm;

        do {
            idTerm = String.valueOf(++idTermNum);
        } while (termRepository.findByIdTermAndIdThesaurus(idTerm, term.getIdThesaurus()).isPresent());

        term.setIdTerm(idTerm);
        return idTerm;
    }

    public void updateTermTraduction(Term term, int idUser) {
        updateTermTraduction(term.getLexicalValue(), term.getIdTerm(), term.getLang(), term.getIdThesaurus(), idUser);
    }

    public void updateTermTraduction(String label, String idTerm, String idLang, String idTheso, int idUser) {
        log.debug("Mise à jour du term");
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idTheso, idLang);
        if (term.isPresent()) {
            term.get().setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label));
            term.get().setContributor(idUser);
            term.get().setModified(new Date());
            termRepository.save(term.get());

            log.debug("Ajout d'une trace de modification de la traduction {}", term.get().getLexicalValue());
            termHistoriqueRepository.save(TermHistorique.builder()
                    .idTerm(term.get().getIdTerm())
                    .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label))
                    .lang(idLang)
                    .idThesaurus(idTheso)
                    .source("")
                    .action("")
                    .idUser(idUser)
                    .action("UPDATE")
                    .modified(LocalDateTime.now())
                    .build());
        }
    }

    public void addTermTraduction(String label, String idTerm, String idLang, String idTheso, int idUser) {
        label = StringUtils.convertString(label);
        log.debug("Ajout d'un nouveau term traduction {}", label);

        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(label)
                .lang(idLang)
                .idThesaurus(idTheso)
                .source("")
                .status("")
                .contributor(idUser)
                .creator(idUser)
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Ajout d'une trace de création d'un nouveau term {}", label);
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(termSaved.getIdTerm())
                .lexicalValue(termSaved.getLexicalValue())
                .lang(termSaved.getLang())
                .idThesaurus(termSaved.getIdThesaurus())
                .source(termSaved.getSource())
                .status(termSaved.getStatus())
                .idUser(idUser)
                .modified(LocalDateTime.now())
                .action("New")
                .build());
    }

    public void addTerms(NodeTerm nodeTerm, int idUser) {

        if (CollectionUtils.isEmpty(nodeTerm.getNodeTermTraduction())) {
            log.error("Aucune traduction n'est pas présente !");
        }

        log.debug("Début de l'ajout des traductions");
        for (NodeTermTraduction termTraduction : nodeTerm.getNodeTermTraduction()) {

            log.debug("Traduction {}", termTraduction.getLexicalValue());
            addInPermutedTable(nodeTerm, termTraduction);

            var lexicalValue = fr.cnrs.opentheso.utils.StringUtils.convertString(termTraduction.getLexicalValue());

            termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                    .idTerm(nodeTerm.getIdTerm())
                    .lexicalValue(lexicalValue)
                    .lang(termTraduction.getLang())
                    .idThesaurus(nodeTerm.getIdThesaurus())
                    .source(nodeTerm.getSource())
                    .status(nodeTerm.getStatus())
                    .contributor(idUser)
                    .created(nodeTerm.getCreated())
                    .modified(nodeTerm.getModified())
                    .build());
        }

        log.debug("Ajout du lien avec preferred term {}", nodeTerm.getIdTerm());
        preferredTermRepository.save(PreferredTerm.builder()
                .idTerm(nodeTerm.getIdTerm())
                .idThesaurus(nodeTerm.getIdThesaurus())
                .idConcept(nodeTerm.getIdConcept())
                .build());
    }

    private void addInPermutedTable(NodeTerm nodeTerm, NodeTermTraduction termTraduction) {

        log.debug("Recherche de l'id Groupe");
        var conceptGroup = conceptGroupConceptRepository.findByIdThesaurusAndIdConcept(nodeTerm.getIdThesaurus(), nodeTerm.getIdConcept());
        if (CollectionUtils.isEmpty(conceptGroup)) {
            log.debug("Aucun group n'est associé au thésaurus id {} et concept id {}", nodeTerm.getIdThesaurus(), nodeTerm.getIdConcept());
            return;
        }
        var idGroup = conceptGroup.get(0).getIdGroup();
        log.debug("Id Group trouvé : {}", idGroup);

        var value = formatLexicalValue(termTraduction.getLexicalValue());
        log.debug("Formatage de lexicalValue : {}", value);

        var tabMots = value.split(" ");
        for (int index = 1; index < tabMots.length; index++) {
            log.debug("Enregistrement dans la table permuted pour lexicalValue {} ({}/{})", tabMots[index], index, tabMots.length);
            permutedRepository.save(Permuted.builder()
                    .ord(index++)
                    .idConcept(nodeTerm.getIdConcept())
                    .idGroup(idGroup)
                    .idThesaurus(nodeTerm.getIdThesaurus())
                    .idLang(termTraduction.getLang())
                    .lexicalValue(tabMots[index])
                    .isPreferredTerm(true)
                    .originalValue(value)
                    .build());
        }
    }

    private String formatLexicalValue(String lexicalValue) {

        var value = lexicalValue.replaceAll("-", " ");
        value = value.replaceAll("\\(", " ");
        value = value.replaceAll("\\)", " ");
        value = value.replaceAll("/", " ");
        return fr.cnrs.opentheso.utils.StringUtils.convertString(value.trim());
    }

    public boolean addLinkTerm(String idConcept, String idThesaurus, String idTerm) {
        log.debug("Ajout d'une relation avec Terme Préféré");
        preferredTermRepository.save(PreferredTerm.builder()
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .idTerm(idTerm)
                .build());
        return true;
    }

    public boolean isTermExistInLangAndThesaurus(String idTerm, String idThesaurus, String idLang) {

        log.debug("Vérification de l'existence du term {} dans le thésaurus {} ({})", idTerm, idThesaurus, idLang);
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idThesaurus, idLang);
        return term.isPresent();
    }

    @Transactional
    public void deleteTerm(String idTerm, String idThesaurus) {

        log.debug("Suppression du terme");
        termRepository.deleteByIdTermAndIdThesaurus(idTerm, idThesaurus);

        log.debug("Suppression de la relation Term_Concept");
        preferredTermRepository.deleteByIdThesaurusAndIdTerm(idThesaurus, idTerm);

        log.debug("Suppression des synonymes");
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTerm(idThesaurus, idTerm);
    }

    public void deleteAllTermsInThesaurus(String idThesaurus) {

        log.debug("Suppression des relations Term_Concept");
        preferredTermRepository.deleteByIdThesaurus(idThesaurus);

        log.debug("Suppression des synonymes");
        nonPreferredTermRepository.deleteByIdThesaurus(idThesaurus);

        log.debug("Suppression du historique des synonymes");
        nonPreferredTermHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression de tous les termes présents dans le thésaurus id {}", idThesaurus);
        termRepository.deleteByIdThesaurus(idThesaurus);

        log.debug("Suppression des traces de terms");
        termHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);
    }

    public void updateThesaurusId(String newIdThesaurus, String oldIdThesaurus) {

        log.debug("Suppression des relations Term_Concept");
        preferredTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Suppression des synonymes");
        nonPreferredTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Suppression du historique des synonymes");
        nonPreferredTermHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Suppression de tous les termes présents dans le thésaurus");
        termRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Suppression des traces de terms");
        termHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public void addTermTraduction(Term term, int idUser) {

        log.debug("Ajout d'un nouveau term traduction {}", term.getLexicalValue());

        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(StringUtils.convertString(term.getLexicalValue()))
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .contributor(term.getContributor())
                .creator(term.getCreator())
                .created(new Date())
                .modified(new Date())
                .build());

        log.debug("Ajout d'une trace de création d'un nouveau term {}", term.getLexicalValue());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(termSaved.getIdTerm())
                .lexicalValue(termSaved.getLexicalValue())
                .lang(termSaved.getLang())
                .idThesaurus(termSaved.getIdThesaurus())
                .source(termSaved.getSource())
                .status(termSaved.getStatus())
                .idUser(idUser)
                .modified(LocalDateTime.now())
                .action("New")
                .build());
    }

    public boolean isTraductionExistOfConcept(String idConcept, String idThesaurus, String idLang) {
        try {
            return termRepository.existsTranslationForConcept(idConcept, idThesaurus, idLang);
        } catch (Exception e) {
            log.error("Error pendant la vérification de l'existence d'une traduction pour le concept: " + idConcept, e);
            return false;
        }
    }

    public boolean isTermExistIgnoreCase(String title, String idThesaurus, String idLang) {
        try {
            String convertedTitle = fr.cnrs.opentheso.utils.StringUtils.convertString(title);
            return termRepository.existsTermIgnoreCase(convertedTitle, idThesaurus, idLang);
        } catch (Exception e) {
            log.error("Error pendant la vérification de l'existence d'un term (ignore case) pour " + title, e);
            return false;
        }
    }

    public boolean existsPrefLabel(String termValue, String idLang, String idThesaurus) {

        log.debug("Vérifier l'existence d'un term {}  (langue {}) dans le thésaurus id {}", termValue, idLang, idThesaurus);
        return termRepository.existsPrefLabel(termValue, idLang, idThesaurus);
    }

    public Term getThisTerm(String idConcept, String idThesaurus, String idLang) {

        if (isTraductionExistOfConcept(idConcept, idThesaurus, idLang)) {
            var result = termRepository.getPreferredTermWithConceptInfo(idConcept, idThesaurus, idLang);
            if (result instanceof Object[] row) {
                return Term.builder()
                        .idTerm((String) row[0])
                        .idConcept((String) row[1])
                        .lexicalValue((String) row[2])
                        .lang((String) row[3])
                        .idThesaurus((String) row[4])
                        .created(Date.from((Instant) row[5]))
                        .modified(Date.from((Instant) row[6]))
                        .source((String) row[7])
                        .status((String) row[8])
                        .contributor(row[9] != null ? (Integer)row[9] : -1)
                        .creator(row[10] != null ? (Integer)row[10] : -1)
                        .build();
            }
        } else {

            var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus); // à créer

            if (concept.isPresent()) {
                return Term.builder()
                        .idTerm("")
                        .idConcept(idConcept)
                        .lexicalValue("")
                        .lang(idLang)
                        .idThesaurus(idThesaurus)
                        .created(concept.get().getCreated())
                        .modified(concept.get().getModified())
                        .status(concept.get().getStatus())
                        .build();
            }
        }

        return null;
    }

    public List<NodeTermTraduction> getTraductionsOfConcept(String idConcept, String idThesaurus, String idLang) {

        var rawResults = termRepository.getConceptTranslationsRaw(idConcept, idThesaurus, idLang);
        List<NodeTermTraduction> results = new ArrayList<>();

        for (Object[] row : rawResults) {
            results.add(new NodeTermTraduction(
                    (String) row[0],     // lang
                    (String) row[1],     // lexicalValue
                    (String) row[2],     // codePays
                    (String) row[3]      // nomLang (case when)
            ));
        }

        return results;
    }

    public void deleteTerm(String idThesaurus, String idTerm, String idLang) {
        log.debug("Suppression d'un term id {}", idTerm);
        termRepository.deleteByIdTermAndLangAndIdThesaurus(idTerm, idLang, idThesaurus);
    }

    public List<String> searchDistinctLangInThesaurus(String idThesaurus) {

        log.debug("Recherche des langues utilisées par le thésaurus id {}", idThesaurus);
        var langues = termRepository.searchDistinctLangInThesaurus(idThesaurus);
        if (CollectionUtils.isEmpty(langues)) {
            log.debug("Aucune langue n'est utilisée par le thésaurus id {}", idThesaurus);
            return List.of();
        }

        log.debug("{} langues utilisées par le thésaurus id {}", langues.size(), idThesaurus);
        return langues;
    }

    public String getLexicalValueOfConcept(String idConcept, String idThesaurus, String idLang) {

        log.debug("Recherche du nom du concept avec l'id {}", idConcept);
        Optional<String> label = termRepository.getLexicalValueOfConcept(idConcept, idThesaurus, idLang);
        if (label.isEmpty()) {
            log.debug("Aucun nom n'est trouvé pour le concept avec l'id {}", idConcept);
            return "";
        }

        log.debug("Le nom du concept dont l'id {} est {}", idConcept, label.get());
        return label.get();
    }

    public void updateIntitule(String intitule, String idTerm, String idThesaurus, String lang) {

        log.debug("Mise à jour de la valeur du term avec id {}", idTerm);
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idThesaurus, lang);
        if (term.isEmpty()) {
            log.error("Aucun term n'est trouvé avec l'id {} et id thésaurus {}", idTerm, idThesaurus);
            return;
        }

        term.get().setLexicalValue(intitule);
        termRepository.save(term.get());
        log.debug("Mise à jour du term id {} terminée", idTerm);
    }

    public List<String> getSynonymesParConcept(String idConcept, String idThesaurus, String idLang){

        log.debug("Recherche des synonymes pour le concept '{}', thésaurus '{}', langue '{}'", idConcept, idThesaurus, idLang);
        var synonymes = nonPreferredTermRepository.findNonPreferredTerms(idConcept, idThesaurus, idLang);
        if(CollectionUtils.isEmpty(synonymes)) {
            log.debug("Aucun synonymes n'est trouvé pour le concept id {}", idConcept);
            return new ArrayList<>();
        }

        log.debug("Nombre de non-preferred terms récupérés : {}", synonymes.size());
        return synonymes.stream().map(NodeEMProjection::getLexicalValue).toList();
    }

    @Transactional
    public void addSynonyme(String intitule, String idThesaurus, String lang, String idTerm) {

        log.debug("Ajout d'un nouveau synonymes");
        nonPreferredTermRepository.save(NonPreferredTerm.builder()
                .lexicalValue(intitule)
                .lang(lang)
                .idThesaurus(idThesaurus)
                .hiden(false)
                .idTerm(idTerm)
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public void addNewTerme(Term term) {

        log.debug("Ajout d'un nouveau terme");
        term.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(term.getLexicalValue()));
        termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(term.getLexicalValue())
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .status(term.getStatus())
                .contributor(term.getContributor())
                .creator(term.getCreator())
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public boolean isAltLabelExist(String title, String idThesaurus, String idLang) {

        log.debug("Vérifier si le synonyme {} existe dans le thésaurus {}", title, idThesaurus);
        return nonPreferredTermRepository.isAltLabelExist(title, idThesaurus, idLang);
    }

    public PreferredTerm getPreferredTermByThesaurusAndConcept(String idThesaurus, String idConcept) {

        var preference = preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        if(preference.isEmpty()) {
            log.error("Aucune term de préférence trouvée pour le concept id {}", idConcept);
            return null;
        }
        return preference.get();
    }

    public fr.cnrs.opentheso.entites.Term getTermByIdAndThesaurusAndLang(String idTerm, String idThesaurus, String idLang) {

        log.debug("Recherche de term par son id {}", idTerm);
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idThesaurus, idLang);
        if (term.isEmpty()) {
            log.error("Aucun term n'est trouvé");
            return null;
        }
        return term.get();
    }

    public List<NodeTermTraduction> getTraductionByConcept(String idThesaurus, String idConcept) {

        log.debug("Recherche des traductions pour le concept id {}", idConcept);
        var traductions = termRepository.findAllTraductionsOfConcept(idConcept, idThesaurus);
        if (CollectionUtils.isEmpty(traductions)) {
            log.debug("Aucune traduction trouvée pour le concept id {}", idConcept);
            return List.of();
        }
        return traductions;
    }

    public String getConceptIdFromPrefLabel(String label, String idThesaurus, String lang) {
        return preferredTermRepository.findConceptIdByLabel(label.replace("'", "%"), idThesaurus, lang)
                .orElse(null);
    }
 }
