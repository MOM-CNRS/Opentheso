package fr.cnrs.opentheso.v2.candidat.alignment;

import fr.cnrs.opentheso.client.alignement.AgrovocHelper;
import fr.cnrs.opentheso.client.alignement.GemetHelper;
import fr.cnrs.opentheso.client.alignement.GeoNamesHelper;
import fr.cnrs.opentheso.client.alignement.GettyAATHelper;
import fr.cnrs.opentheso.client.alignement.IdRefHelper;
import fr.cnrs.opentheso.client.alignement.OntomeHelper;
import fr.cnrs.opentheso.client.alignement.OpenthesoHelper;
import fr.cnrs.opentheso.client.alignement.WikidataHelper;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlignmentAutoExternalSearch {

    public record SearchOutcome(List<NodeAlignment> results, String infoDetail) {}

    public SearchOutcome search(AlignementSource source, SearchContext context) {
        if (source == null) {
            return new SearchOutcome(null, "Pas de source sélectionnée");
        }
        String filter = source.getSource_filter();
        if (filter == null) {
            return new SearchOutcome(List.of(), null);
        }

        return switch (filter) {
            case "wikidata_sparql" -> searchWikidataSparql(source, context);
            case "wikidata_rest" -> searchWikidataRest(source, context);
            case "idRefSujets" -> searchIdRefSubject(source, context);
            case "idRefPersonnes" -> searchIdRefPerson(source, context);
            case "idRefAuteurs" -> searchIdRefNames(source, context);
            case "idRefLieux" -> searchIdRefLieux(source, context);
            case "IdRefTitreUniforme" -> searchIdRefUniformTitle(source, context);
            case "Getty_AAT" -> searchGettyAat(source, context);
            case "Opentheso" -> searchOpentheso(source, context);
            case "Gemet" -> searchGemet(source, context);
            case "Agrovoc" -> searchAgrovoc(source, context);
            case "GeoNames" -> searchGeoNames(source, context);
            case "Ontome" -> searchOntome(source, context);
            default -> new SearchOutcome(List.of(), null);
        };
    }

    public record SearchContext(
            String thesaurusId,
            String conceptId,
            String lexicalValue,
            String lang,
            String nom,
            String prenom
    ) {}

    private SearchOutcome searchWikidataRest(AlignementSource source, SearchContext context) {
        WikidataHelper helper = new WikidataHelper();
        List<NodeAlignment> results = helper.queryWikidata_rest(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchWikidataSparql(AlignementSource source, SearchContext context) {
        WikidataHelper helper = new WikidataHelper();
        String query = source.getRequete()
                .replace("##lang##", context.lang())
                .replace("##value##", context.lexicalValue());
        source.setRequete(query);
        List<NodeAlignment> results = helper.queryWikidata_sparql(
                context.conceptId(), context.thesaurusId(), query, source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchIdRefSubject(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefSubject(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefPerson(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefPerson(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefNames(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefNames(
                context.conceptId(), context.thesaurusId(), context.nom(), context.prenom(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefUniformTitle(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefUniformtitle(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefLieux(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefLieux(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGettyAat(AlignementSource source, SearchContext context) {
        GettyAATHelper helper = new GettyAATHelper();
        List<NodeAlignment> results = helper.queryAAT(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchOpentheso(AlignementSource source, SearchContext context) {
        OpenthesoHelper helper = new OpenthesoHelper();
        List<NodeAlignment> results = helper.queryOpentheso(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGemet(AlignementSource source, SearchContext context) {
        GemetHelper helper = new GemetHelper();
        List<NodeAlignment> results = helper.queryGemet(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchAgrovoc(AlignementSource source, SearchContext context) {
        AgrovocHelper helper = new AgrovocHelper();
        List<NodeAlignment> results = helper.queryAgrovoc(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGeoNames(AlignementSource source, SearchContext context) {
        GeoNamesHelper helper = new GeoNamesHelper();
        List<NodeAlignment> results = helper.queryGeoNames(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchOntome(AlignementSource source, SearchContext context) {
        OntomeHelper helper = new OntomeHelper();
        List<NodeAlignment> results = helper.queryOntomeHelper(
                context.conceptId(), context.thesaurusId(), context.lexicalValue().trim(),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome outcome(List<NodeAlignment> results, String detail) {
        if (results == null) {
            return new SearchOutcome(null, detail);
        }
        return new SearchOutcome(results, null);
    }
}
