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
import org.apache.commons.lang3.StringUtils;
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
        if (StringUtils.isBlank(filter)) {
            return new SearchOutcome(List.of(), null);
        }

        // Les valeurs en base sont hétérogènes (ex. Wikidata_rest, IdRefSujets) :
        // même normalisation que le legacy (SearchAllignementByConceptCallable).
        return switch (filter.toUpperCase()) {
            case "WIKIDATA_SPARQL" -> searchWikidataSparql(source, context);
            case "WIKIDATA_REST" -> searchWikidataRest(source, context);
            case "IDREFSUJETS" -> searchIdRefSubject(source, context);
            case "IDREFPERSONNES" -> searchIdRefPerson(source, context);
            case "IDREFAUTEURS" -> searchIdRefNames(source, context);
            case "IDREFLIEUX" -> searchIdRefLieux(source, context);
            case "IDREFTITREUNIFORME" -> searchIdRefUniformTitle(source, context);
            case "GETTY_AAT" -> searchGettyAat(source, context);
            case "OPENTHESO" -> searchOpentheso(source, context);
            case "GEMET" -> searchGemet(source, context);
            case "AGROVOC" -> searchAgrovoc(source, context);
            case "GEONAMES" -> searchGeoNames(source, context);
            case "ONTOME" -> searchOntome(source, context);
            default -> new SearchOutcome(List.of(), "Filtre de source non supporté : " + filter);
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
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchWikidataSparql(AlignementSource source, SearchContext context) {
        WikidataHelper helper = new WikidataHelper();
        // Ne pas muter source.requete : la même instance est réutilisée en batch.
        String query = StringUtils.defaultString(source.getRequete())
                .replace("##lang##", StringUtils.defaultString(context.lang()))
                .replace("##value##", StringUtils.defaultString(context.lexicalValue()));
        List<NodeAlignment> results = helper.queryWikidata_sparql(
                context.conceptId(), context.thesaurusId(), query, source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchIdRefSubject(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefSubject(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefPerson(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefPerson(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefNames(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefNames(
                context.conceptId(), context.thesaurusId(),
                StringUtils.defaultString(context.nom()),
                StringUtils.defaultString(context.prenom()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefUniformTitle(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefUniformtitle(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchIdRefLieux(AlignementSource source, SearchContext context) {
        IdRefHelper helper = new IdRefHelper();
        List<NodeAlignment> results = helper.queryIdRefLieux(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGettyAat(AlignementSource source, SearchContext context) {
        GettyAATHelper helper = new GettyAATHelper();
        List<NodeAlignment> results = helper.queryAAT(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchOpentheso(AlignementSource source, SearchContext context) {
        OpenthesoHelper helper = new OpenthesoHelper();
        List<NodeAlignment> results = helper.queryOpentheso(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGemet(AlignementSource source, SearchContext context) {
        GemetHelper helper = new GemetHelper();
        List<NodeAlignment> results = helper.queryGemet(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages().toString());
    }

    private SearchOutcome searchAgrovoc(AlignementSource source, SearchContext context) {
        AgrovocHelper helper = new AgrovocHelper();
        List<NodeAlignment> results = helper.queryAgrovoc(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchGeoNames(AlignementSource source, SearchContext context) {
        GeoNamesHelper helper = new GeoNamesHelper();
        List<NodeAlignment> results = helper.queryGeoNames(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                context.lang(), source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private SearchOutcome searchOntome(AlignementSource source, SearchContext context) {
        OntomeHelper helper = new OntomeHelper();
        List<NodeAlignment> results = helper.queryOntomeHelper(
                context.conceptId(), context.thesaurusId(), safeTrim(context.lexicalValue()),
                source.getRequete(), source.getSource());
        return outcome(results, helper.getMessages());
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private SearchOutcome outcome(List<NodeAlignment> results, String detail) {
        if (results == null) {
            return new SearchOutcome(null, detail);
        }
        return new SearchOutcome(results, null);
    }
}
