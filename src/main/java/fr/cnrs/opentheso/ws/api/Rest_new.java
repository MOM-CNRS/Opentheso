package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.models.thesaurus.NodeThesaurus;
import fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.Json;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Web Service
 *
 * @author miled.rousset
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@CrossOrigin(methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.PUT })
@Tag(name = "Ancienne API", description = "Anciennes requêtes API REST")
public class Rest_new {

    private final TermRepository termRepository;
    private final D3jsHelper d3jsHelper;
    private final RestRDFHelper restRDFHelper;
    private final GroupService groupService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;

    private static final String JSON_FORMAT = "application/json";
    private static final String JSON_FORMAT_LONG = JSON_FORMAT + ";charset=UTF-8";

    // Mapping des formats à leurs MIME types correspondants
    private static final Map<String, String> FORMAT_MAP = Map.of(
            "rdf", CustomMediaType.APPLICATION_RDF,
            "jsonld", CustomMediaType.APPLICATION_JSON_LD,
            "turtle", CustomMediaType.APPLICATION_TURTLE,
            "json", JSON_FORMAT
    );

/////////////////////////////////////////////////////
/////////////////////////////////////////////////////
    /*
     * recherche par Id Ark
     * Partie pour la négociation de contenu
     * concernant les URI de type ARK avec header
     * curl -L --header "Accept: application/rdf+xml »
     * curl -L --header "Accept: text/turtle »
     * curl -L --header "Accept: application/json »
     * curl -L --header "Accept: application/ld+json »
     */
/////////////////////////////////////////////////////
/////////////////////////////////////////////////////

    @GetMapping(value = "/{naan}/{idArk}.rdf", produces = CustomMediaType.APPLICATION_RDF)
    public ResponseEntity<String> getSkosFromArk(@PathVariable("naan") String naan,
                                                 @PathVariable("idArk") String arkId) {
        return processRequest(naan, arkId, CustomMediaType.APPLICATION_RDF);
    }

    @GetMapping(value = "/{naan}/{idArk}.json", produces = JSON_FORMAT)
    public ResponseEntity<String> getJsonFromArk(@PathVariable("naan") String naan,
                                                 @PathVariable("idArk") String arkId) {
        return processRequest(naan, arkId, JSON_FORMAT);
    }

    @GetMapping(value = "/{naan}/{idArk}.jsonld", produces = CustomMediaType.APPLICATION_JSON_LD)
    public ResponseEntity<String> getJsonldFromArk(@PathVariable("naan") String naan,
                                                   @PathVariable("idArk") String arkId) {
        return processRequest(naan, arkId, CustomMediaType.APPLICATION_JSON_LD);
    }

    @GetMapping(value = "/{naan}/{idArk}.ttl", produces = CustomMediaType.APPLICATION_TURTLE)
    public ResponseEntity<String> getTurtleFromArk(@PathVariable("naan") String naan,
                                                   @PathVariable("idArk") String arkId) {
        return processRequest(naan, arkId, CustomMediaType.APPLICATION_TURTLE);
    }

    // Méthode privée pour gérer les requêtes
    private ResponseEntity<String> processRequest(String naan, String arkId, String mimeType) {
        if (StringUtils.isEmpty(naan) || StringUtils.isEmpty(arkId)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(restRDFHelper.exportConcept(naan + "/" + arkId, mimeType));
    }

/////////////////////////////////////////////////////
    /*
     * fin de la recherche par idArk
     */
/////////////////////////////////////////////////////
    /**
     * GET concept par DOI et produire du REF-SKOS
     */
    @GetMapping("/doi:{doi1}.{doi2}/{doiId}.rdf")
    public ResponseEntity<Object> getSkosFromDoi__(@PathVariable("doi1") String doi1,
                                           @PathVariable("doi2") String doi2,
                                           @PathVariable("doiId") String doiId) {

        if (StringUtils.isEmpty(doi1) || StringUtils.isEmpty(doi2) || StringUtils.isEmpty(doiId)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var datas = restRDFHelper.exportConceptDoi("doi:" + doi1 + "." + doi2 + "/" + doiId, CustomMediaType.APPLICATION_RDF);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_RDF_UTF_8)).body(datas);
    }

    /*
     * recherche par Id Handle
     * Partie pour la négociation de contenu
     * concernant les URI de type Handle avec header
     * curl -L --header "Accept: application/rdf+xml »
     * curl -L --header "Accept: text/turtle »
     * curl -L --header "Accept: application/json »
     * curl -L --header "Accept: application/ld+json »
     */
    /**
     * pour produire du RDF-SKOS
     */
    @GetMapping(value = "/{hdl1}.{hdl2}.{hdl3}/{handleId}.rdf", produces = CustomMediaType.APPLICATION_RDF_UTF_8)
    public ResponseEntity<Object> getSkosFromHandle__(@PathVariable("hdl1") String hdl1,
                                              @PathVariable("hdl2") String hdl2,
                                              @PathVariable("hdl3") String hdl3,
                                              @PathVariable("handleId") String handleId) {

        return getResponseEntity(hdl1, hdl2, hdl3, handleId, CustomMediaType.APPLICATION_RDF, CustomMediaType.APPLICATION_RDF_UTF_8);
    }

    /**
     * pour produire du Json
     */
    @GetMapping(value = "/{hdl1}.{hdl2}.{hdl3}/{handleId}.json", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getJsonFromHandle__(@PathVariable("hdl1") String hdl1,
                                              @PathVariable("hdl2") String hdl2,
                                              @PathVariable("hdl3") String hdl3,
                                              @PathVariable("handleId") String handleId) {

        return getResponseEntity(hdl1, hdl2, hdl3, handleId, JSON_FORMAT, JSON_FORMAT_LONG);
    }

    /**
     * pour produire du JsonLd
     */
    @GetMapping(value = "/{hdl1}.{hdl2}.{hdl3}/{handleId}.jsonld", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getJsonldFromHandle__(@PathVariable("hdl1") String hdl1,
                                                @PathVariable("hdl2") String hdl2,
                                                @PathVariable("hdl3") String hdl3,
                                                @PathVariable("handleId") String handleId) {

        return getResponseEntity(hdl1, hdl2, hdl3, handleId, CustomMediaType.APPLICATION_JSON_LD, CustomMediaType.APPLICATION_JSON_LD_UTF_8);
    }

    /**
     * pour produire du Turtle
     */
    @GetMapping(value = "/{hdl1}.{hdl2}.{hdl3}/{handleId}.ttl", produces = CustomMediaType.APPLICATION_TURTLE_UTF_8)
    public ResponseEntity<Object> getTurtleFromHandle__(@PathVariable("hdl1") String hdl1,
                                                @PathVariable("hdl2") String hdl2,
                                                @PathVariable("hdl3") String hdl3,
                                                @PathVariable("handleId") String handleId) {

        return getResponseEntity(hdl1, hdl2, hdl3, handleId, CustomMediaType.APPLICATION_TURTLE, CustomMediaType.APPLICATION_TURTLE_UTF_8);
    }

    @NotNull
    private ResponseEntity getResponseEntity(String hdl1, String hdl2, String hdl3, String handleId, String rdfFormat, String rdfFormatLong) {
        if (StringUtils.isEmpty(hdl1) || StringUtils.isEmpty(hdl2) || StringUtils.isEmpty(hdl3) || StringUtils.isEmpty(handleId)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var data = restRDFHelper.exportConceptHdl(hdl1 + "." + hdl2 + "." + hdl3 + "/" + handleId, rdfFormat);

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(rdfFormatLong)).body(data);
    }

/////////////////////////////////////////////////////
// fin de la recherche par idHandle
/////////////////////////////////////////////////////

/////////////////////////////////////////////////////
// Recherche par Id du concept
/////////////////////////////////////////////////////

    //Produire du RDF-SKOS
    @GetMapping(value = "/{idTheso}.{idConcept}.rdf", produces = CustomMediaType.APPLICATION_RDF_UTF_8)
    public ResponseEntity<Object> getSkosFromIdConcept(@PathVariable("idTheso") String idTheso, @PathVariable("idConcept") String idConcept) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_RDF_UTF_8))
                .body(restRDFHelper.exportConceptFromId(idConcept, idTheso, CustomMediaType.APPLICATION_RDF));
    }

    //Produire du RDF par defaut
    @GetMapping(value = "/{idTheso}.{idConcept}", produces = CustomMediaType.APPLICATION_RDF_UTF_8)
    public ResponseEntity<Object> getJsonFromIdConcept__(@PathVariable("idTheso") String idTheso, @PathVariable("idConcept") String idConcept) {

        var data = restRDFHelper.exportConceptFromId(idConcept, idTheso, CustomMediaType.APPLICATION_RDF_UTF_8);
        if (data == null) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_RDF_UTF_8))
                .body(data);
    }

    //Produire du Json
    @GetMapping(value = "/{idTheso}.{idConcept}.json", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getJsonFromIdConcept(@PathVariable("idTheso") String idTheso, @PathVariable("idConcept") String idConcept) {

        if (StringUtils.isEmpty(idConcept)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(restRDFHelper.exportConceptFromId(idConcept, idTheso, JSON_FORMAT));
    }

    /**
     * pour produire du JsonLd
     */
    @GetMapping(value = "/{idTheso}.{idConcept}.jsonld", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getJsonLdFromIdConcept(@PathVariable("idTheso") String idTheso, @PathVariable("idConcept") String idConcept) {

        if (StringUtils.isEmpty(idConcept)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_JSON_LD_UTF_8))
                .body(restRDFHelper.exportConceptFromId(idConcept, idTheso, "application/ld+json"));
    }

    /**
     * pour produire du Turtle
     */
    @GetMapping(value = "/{idTheso}.{idConcept}.ttl", produces = CustomMediaType.APPLICATION_TURTLE_UTF_8)
    public ResponseEntity<Object> getTurtleFromIdConcept(@PathVariable("idTheso") String idTheso, @PathVariable("idConcept") String idConcept) {

        if (StringUtils.isEmpty(idConcept)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var data = restRDFHelper.exportConceptFromId(idConcept, idTheso, CustomMediaType.APPLICATION_TURTLE);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_TURTLE_UTF_8))
                .body(data);
    }

/////////////////////////////////////////////////////
//Fin de la recherche par Id du concept
/////////////////////////////////////////////////////

/////////////////////////////////////////////////////
// Trouver la valeur d'après un ID Ark concept
/////////////////////////////////////////////////////

    //Produire du Json
    @GetMapping(value = "/preflabel.{idLang}/{naan}/{idArk}.json", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getPrefLabelJsonFromArk(@PathVariable("idLang") String idLang,
                                                  @PathVariable("naan") String naan,
                                                  @PathVariable("idArk") String arkId) {

        if (StringUtils.isEmpty(naan) || StringUtils.isEmpty(arkId) || StringUtils.isEmpty(idLang)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(restRDFHelper.getPrefLabelFromArk(naan, arkId, idLang));
    }

/////////////////////////////////////////////////////
// Recherche par valeurs avec négociation de contenu
/////////////////////////////////////////////////////

    /**
     * Permet de rechercher une valeur en filtrant par theso et par langue avec
     * négociation de contenu //exp curl -L --header "Accept:
     * application/rdf+xml"
     * http://localhost:8083/opentheso/api/search?q="vase&lang=fr&theso=2" curl
     * http://localhost:8083/opentheso/api/search?q=notation:nota1&theso=1&format=json
     * curl -L --header "Accept: application/rdf+xml"
     * http://localhost:8083/opentheso/api/search?q=notation:nota1&theso=1
     *
     * /// options
     * https://pactols.frantiq.fr/opentheso/api/search?q=ark:/26678/pcrtVFfTq3JlGu&lang=fr&theso=TH_1&showLabels=true
     *
     */
    @GetMapping(value = "/search")
    public ResponseEntity<Object> searchRdf(@RequestParam("theso") String idTheso,
                                    @RequestParam(value = "idLang", required = false) String idLang,
                                    @RequestParam("q") String value,
                                    @RequestParam(value = "showLabels", required = false, defaultValue = "false") boolean showLabels,
                                    @RequestParam(value = "groups", required = false) String groups,
                                    @RequestParam(value = "match", required = false) String match,
                                    @RequestParam(value = "format", required = false) String format, // ne pas supprimer; elle sert à filtrer le format du résultat pour OmekaS
                                    @RequestHeader(value = "accept", required = false) String acceptHeader) {

        if (!value.contains("ark:/") && StringUtils.isEmpty(idTheso)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }
        // match=exact (pour limiter la recherche aux termes exactes) match=exactone (pour chercher les prefLable, s'il n'existe pas, on cherche sur les altLabels

        String [] groupList = null; // group peut être de la forme suivante pour multiGroup (G1,G2,G3)
        String formatFiltered;
        if(StringUtils.isNotEmpty(format)) {
            formatFiltered = format;
        } else{
            switch (acceptHeader.toLowerCase()) {
                case CustomMediaType.APPLICATION_JSON_LD:
                    formatFiltered= "jsonld";
                    break;
                case JSON_FORMAT:
                    formatFiltered= "json";
                    break;
                case CustomMediaType.APPLICATION_TURTLE:
                    formatFiltered= "turtle";
                    break;
                default:
                    formatFiltered= "rdf";
                    break;
            }
        }
        if(StringUtils.isNotEmpty(groups)){
            groupList = groups.split(",");
        }

        /// rercherche par idArk
        if (value.contains("ark:/")) {
            return ResponseEntity.ok(getDatasFromArk(idTheso, idLang, value.substring(value.indexOf("ark:/")+5), showLabels));
        } else {
            // vérification du filtre pour savoir si la recherche concerne des champs spécifiques
            String filter = null;
            if (value.contains("notation:")) {
                filter = "notation:";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(FORMAT_MAP.getOrDefault(formatFiltered, JSON_FORMAT)))//CustomMediaType.APPLICATION_RDF_UTF_8))
                    .body(getDatas(idTheso, idLang, groupList, value, FORMAT_MAP.getOrDefault(formatFiltered, JSON_FORMAT), filter, match));
        }
    }

    private String getDatas(String idTheso, String idLang, String [] groups, String value, String format, String filter, String match) {
        if (filter != null) {
            if ("notation:".equalsIgnoreCase(filter)) {
                value = value.substring(value.indexOf(":") + 1);
                return restRDFHelper.findNotation(idTheso, value, format);
            }
        }
        return restRDFHelper.findConcepts(idTheso, idLang, groups, value, format, match);
    }

    private String getDatasFromArk(String idTheso, String idLang, String idArk, boolean showLabels) {

        return StringUtils.isEmpty(idLang)
                ? restRDFHelper.exportConcept(idArk, JSON_FORMAT)
                : restRDFHelper.exportConceptFromArkWithLang(idArk, idTheso, idLang, showLabels, JSON_FORMAT);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////Fonction qui permet de produire /////////////////////////////////////////  
    //////////////des données Json pour le widget Aïoli////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * Permet de rechercher une valeur en filtrant par theso, groupe et langue
     *
     * "http://193.48.140.131:8083/opentheso/api/searchwidget?q=or&lang=fr&theso=TH_1"
     */
    @GetMapping(value = "/searchwidget", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> searchJsonForWidget(@RequestParam(required = false, value = "lang") String idLang,
                                              @RequestParam(value = "theso") String idTheso,
                                              @RequestParam(required = false, value = "group") String groupValue,
                                              @RequestParam(required = false, value = "arkgroup") String arkGroupValue ,
                                              @RequestParam(required = false, value = "format") String format,
                                              @RequestParam(value = "q", required = false) String value,
                                              @RequestParam(required = false, value = "match") boolean match) {
        // format = full (on renvoie les altLabel en plus)
        if (StringUtils.isEmpty(idTheso)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage("l'Id du Thesaurus est obligatoire"));
        }

        String[] groups = null; // group peut être de la forme suivante pour multiGroup (G1,G2,G3)
        if (StringUtils.isNotEmpty(groupValue)) {
            groups = groupValue.split(",");
        }

        String[] arkGroups = null; // group peut être de la forme suivante pour multiGroup (psrbfdfdjsfh,fdsfdsfsf,kdhfjsdfhjhf)
        if (StringUtils.isNotEmpty(arkGroupValue)) {
            arkGroups = arkGroupValue.split(",");
        }
        if(ArrayUtils.isNotEmpty(arkGroups)){
            groups = getIdGroupFromArk(arkGroups, idTheso);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(restRDFHelper.findDatasForWidget(idTheso, idLang, groups, value, format, match));
    }

    private String[] getIdGroupFromArk(String[] arkGroups, String idTheso) {
        String[] groups = new String[arkGroups.length];
        int i=0;
        for (String arkGroup : arkGroups) {
            groups[i] = groupService.getIdGroupFromArkId(arkGroup, idTheso);
            i++;
        }
        return groups;
    }    

    
    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////Fonction qui permet de produire /////////////////////////////////////////  
    //////////////des données Json pour le widget Koha////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * Permet de rechercher une valeur en filtrant par theso, groupe et langue
     *
     * "http://193.48.140.131:8083/opentheso/api/searchwidgetbyark?q=77777/abcdddor,76767/oreijezrnh&lang=fr"
     */
    @GetMapping(value = "/searchwidgetbyark", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> searchJsonForWidgetByArk(@RequestParam(value = "lang") String idLang,
                                                   @RequestParam(required = false, value = "format") String format,
                                                   @RequestParam(value = "q") String query) {
        // format = full (on renvoie les altLabel en plus)
        String[] idArks = StringUtils.isEmpty(query) ? null : query.split(",");
        
        if(ArrayUtils.isEmpty(idArks)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage("l'Id Ark est obligatoire"));
        } else {
            JsonArrayBuilder datas = restRDFHelper.findDatasForWidgetByArk(idLang, idArks, format);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(datas.build().toString());
        }
    }
    
/////////////////////////////////////////////////////    
// fin de la recherche par valeur
///////////////////////////////////////////////////// 

/////////////////////////////////////////////////////
// Recherche par valeurs pour autocomplétion on revoie que le prefLable et l'URI en Json
/////////////////////////////////////////////////////

    /**
     * Permet de rechercher une valeur en filtrant par theso et par langue
     * retourne une liste des valeurs (prefLabel + Uri) pour les programmes qui
     * font de l'autocompletion exp :
     * http://193.48.140.131:8083/opentheso/api/autocomplete/or?theso=TH_1&lang=fr&group=6
     */
    @GetMapping(value = "/autocomplete/{value}", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> searchAutocomplete(@PathVariable("value") String value,
                                             @RequestParam(value = "theso") String idTheso,
                                             @RequestParam(value = "lang", required = false) String idLang,
                                             @RequestParam(value = "group", required = false) String group,
                                             @RequestParam(value = "format", required = false) String format) {

        String [] groups = null;
        if(StringUtils.isNotEmpty(group)) {
            groups = group.split(",");// group peut être de la forme suivante pour multiGroup (G1,G2,G3)
        }

        if (StringUtils.isEmpty(value)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage(messageEmptyJson()));
        }

        String datas;
        if (format != null && format.equalsIgnoreCase("full")) {
            datas = restRDFHelper.findAutocompleteConcepts(idTheso, idLang, groups, value, true);
        } else {
            datas = restRDFHelper.findAutocompleteConcepts(idTheso, idLang, groups, value, false);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    /**
     * Une autre forme d'appel pour l'autocomplétion adaptée aux plugins type
     * Omeka-S Permet de rechercher une valeur en filtrant par theso et par
     * langue retourne une liste des valeurs (prefLabel + Uri) pour les
     * programmes qui font de l'autocompletion exp :
     * http://localhost:8080/opentheso/api/autocomplete?theso=TH_1&value=vase&lang=fr&group=6
     * le format : pour définir s'il faut renvoyer plus des données (définition
     * ....)
     */
    @GetMapping(value = "/autocomplete", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> searchAutocomplete2(@RequestParam(value = "value") String value,
                                        @RequestParam(value = "theso") String idTheso,
                                        @RequestParam(value = "lang", required = false) String idLang,
                                        @RequestParam(value = "group", required = false) String group,
                                        @RequestParam(value = "format", required = false) String format) {
        String [] groups = null;
        if(StringUtils.isNotEmpty(group)) {
            groups = group.split(",");
        }
        if (StringUtils.isEmpty(value)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage(messageEmptyJson()));
        }

        String datas;
        if (format != null && format.equalsIgnoreCase("full")) {
            datas = restRDFHelper.findAutocompleteConcepts(idTheso, idLang, groups, value, true);
        } else {
            datas = restRDFHelper.findAutocompleteConcepts(idTheso, idLang, groups, value, false);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

/////////////////////////////////////////////////////
// Fin de la recherche par valeurs pour autocomplétion
/////////////////////////////////////////////////////

///////////////////////////////////////////////////// 
// Fonctions avancées pour retourner une branche complète
/////////////////////////////////////////////////////
    /**
     * Pour retourner une branche complète à partir d'un concept en SKOS mais en
     * remontant la branche par les BT (termes génériques)
     * http://localhost:8082/opentheso2/api/expansion/concept?id=30&theso=th1&way=down
     * http://localhost:8082/opentheso2/api/expansion/concept?id=30&theso=th1&way=down&format=json
     */
    @GetMapping(value = "expansion/concept", produces = CustomMediaType.APPLICATION_RDF_UTF_8)
    public ResponseEntity<Object> getBrancheOfConcepts(@RequestParam(value = "theso") String idTheso,
                                               @RequestParam(value = "id") String idConcept,
                                               @RequestParam(value = "idark", required = false) String idark,
                                               @RequestParam(value = "way", required = false) String way,
                                               @RequestParam(value = "format", required = false, defaultValue = "rdf") String format) {

        if (StringUtils.isEmpty(idTheso)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage(messageEmptyJson()));
        }

        if (StringUtils.isEmpty(idConcept)) {
            if (StringUtils.isEmpty(idark)) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage(messageEmptyJson()));
            } else {
                idConcept = conceptService.getIdConceptFromArkId(idark, idTheso);
                if(idConcept == null)
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage(messageEmptyJson()));
            }
        }

        switch (format) {
            case "rdf": {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_RDF_UTF_8))
                        .body(getBranchOfConcepts(idConcept, idTheso, way, CustomMediaType.APPLICATION_RDF));
            }
            case "jsonld":
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(CustomMediaType.APPLICATION_JSON_LD_UTF_8))
                    .body(getBranchOfConcepts(idConcept, idTheso, way, CustomMediaType.APPLICATION_JSON_LD));
            case "turtle":
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(getBranchOfConcepts(idConcept, idTheso, way, CustomMediaType.APPLICATION_TURTLE));
            default:
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(getBranchOfConcepts(idConcept, idTheso, way, JSON_FORMAT));
        }
    }

    private String getBranchOfConcepts(String idConcept, String idTheso, String way, String format) {
        if (way.equalsIgnoreCase("top")) {
            return restRDFHelper.brancheOfConceptsTop(idConcept, idTheso, format);
        }
        if (way.equalsIgnoreCase("down")) {
            return restRDFHelper.brancheOfConceptsDown(idConcept, idTheso, format);
        }
        return "";
    }

    //Pour retourner une branche complète à partir d'un identifiant d'un groupe
    @GetMapping(value = "all/group", produces = CustomMediaType.APPLICATION_RDF_UTF_8)
    public ResponseEntity<Object> getAllBrancheOfGroup(@RequestParam(value = "theso") String idTheso,
                                               @RequestParam(value = "id") String id,
                                               @RequestParam(value = "format", required = false, defaultValue = "rdf") String format) {

        var groups = id.split(","); // group peut être de la forme suivante pour multiGroup (G1,G2,G3)

        String formatOutput = switch (format) {
            case "rdf" -> {
                format = "application/rdf+xml";
                yield CustomMediaType.APPLICATION_RDF_UTF_8;
            }
            case "jsonld" -> {
                format = "application/ld+json";
                yield CustomMediaType.APPLICATION_JSON_LD_UTF_8;
            }
            case "turtle" -> {
                format = CustomMediaType.APPLICATION_TURTLE;
                yield CustomMediaType.APPLICATION_TURTLE_UTF_8;
            }
            default -> {
                format = JSON_FORMAT;
                yield JSON_FORMAT_LONG;
            }
        };

        var datas = restRDFHelper.brancheOfGroup(groups, idTheso, format);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(formatOutput)).body(datas);
    }

    /**
     * Pour retourner un thesaurus complet à partir de son identifiant
     */
    @GetMapping(value = "all/theso", produces = CustomMediaType.APPLICATION_RDF + ";charset=UTF-8")
    public ResponseEntity<String> getAllTheso(@RequestParam(value = "id") String id,
                                              @RequestParam(value = "format", defaultValue = "rdf") String format) {

        // Obtenir le type MIME en fonction du format, avec une valeur par défaut JSON
        String mimeType = FORMAT_MAP.getOrDefault(format, JSON_FORMAT);

        try {
            // Appel à la méthode pour récupérer le Thesaurus
            String result = restRDFHelper.getTheso(id, mimeType);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimeType)).body(result);
        } catch (Exception e) {
            // Gestion d'une exception, peut-être retourner une réponse d'erreur avec un statut approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"An error occurred while fetching the thesaurus.\"}");
        }
    }
    
    /**
     * Pour retourner un thesaurus complet à partir de son identifiant
     * le thesaurus retourné ne comporte pas de relations, mais uniquement Id et value en Json
     * On peut préciser la langue
     */
    @GetMapping(value = "jsonlist/theso", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getAllIdValueTheso(@RequestParam(value = "id") String idTheso,
                                             @RequestParam(value = "lang") String lang) {
        
        var datas = restRDFHelper.getThesoIdValue(idTheso, lang);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    
/////////////////////////////////////////////////////    
///////////////////////////////////////////////////// 
    /*
     * inforamtions sur le thésaurus
     */
///////////////////////////////////////////////////// 
/////////////////////////////////////////////////////      
    /**
     * Pour retourner la liste des thésaurus publics ou la liste des collections d'un thésaurus
     * https://pactols.frantiq.fr/opentheso/api/info/list?theso=all
     * https://pactols.frantiq.fr/opentheso/api/info/list?theso=th1&group=all
     * https://pactols.frantiq.fr/opentheso/api/info/list?theso=th1&topconcept=all
     */
    @GetMapping(value = "info/list", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getlistAllPublicTheso(@RequestParam(value = "theso") String idTheso,
                                                @RequestParam(value = "group", required = false) String group,
                                                @RequestParam(value = "topconcept", required = false) String topconcept) {

        if (group == null && topconcept == null) {
            return ResponseEntity.ok(getlistAllPublicTheso__());
        } else {
            if (topconcept != null && topconcept.equalsIgnoreCase("all")) {
                return ResponseEntity.ok(getlistAllTopConceptOfTheso__(idTheso));
            }

            return ResponseEntity.ok(getlistAllGroupOfTheso__(idTheso));
        }
    }

    private String getlistAllPublicTheso__() {
        List<String> listPublicTheso = thesaurusService.getAllIdOfThesaurus(false);

        NodeThesaurus nodeThesaurus;

        String datasJson;
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (String idTheso : listPublicTheso) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("idTheso", idTheso);
            JsonArrayBuilder jsonArrayBuilderLang = Json.createArrayBuilder();

            nodeThesaurus = thesaurusService.getNodeThesaurus(idTheso);
            for (Thesaurus thesaurus : nodeThesaurus.getListThesaurusTraduction()) {
                JsonObjectBuilder jobLang = Json.createObjectBuilder();
                jobLang.add("lang", thesaurus.getLanguage());
                jobLang.add("title", thesaurus.getTitle());
                jsonArrayBuilderLang.add(jobLang.build());
            }
            if (!nodeThesaurus.getListThesaurusTraduction().isEmpty()) {
                job.add("labels", jsonArrayBuilderLang.build());
            }
            jsonArrayBuilder.add(job.build());
        }
        datasJson = jsonArrayBuilder.build().toString();

        if (datasJson != null) {
            return datasJson;
        } else {
            return null;
        }
    }

    private String getlistAllGroupOfTheso__(String idTheso) {

        List<String> listIdGroupOfTheso = groupService.getGroupsByThesaurus(idTheso);

        List<NodeGroupTraductions> nodeGroupTraductions;

        String datasJson;
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (String idGroup : listIdGroupOfTheso) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("idGroup", idGroup);
            JsonArrayBuilder jsonArrayBuilderLang = Json.createArrayBuilder();

            nodeGroupTraductions = groupService.getAllGroupTraduction(idGroup, idTheso);
            for (NodeGroupTraductions nodeGroupTraduction : nodeGroupTraductions) {
                JsonObjectBuilder jobLang = Json.createObjectBuilder();
                jobLang.add("lang", nodeGroupTraduction.getIdLang());
                jobLang.add("title", nodeGroupTraduction.getTitle());
                jsonArrayBuilderLang.add(jobLang.build());
            }
            if (!nodeGroupTraductions.isEmpty()) {
                job.add("labels", jsonArrayBuilderLang.build());
            }
            jsonArrayBuilder.add(job.build());
        }
        datasJson = jsonArrayBuilder.build().toString();

        if (datasJson != null) {
            return datasJson;
        } else {
            return null;
        }
    }

    private String getlistAllTopConceptOfTheso__(String idTheso) {

        List<String> listIdTopConceptOfTheso = conceptService.getAllTopConceptIds(idTheso);

        List<NodeTermTraduction> nodeTermTraductions;

        String datasJson;
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (String idConcept : listIdTopConceptOfTheso) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("idConcept", idConcept);
            JsonArrayBuilder jsonArrayBuilderLang = Json.createArrayBuilder();

            nodeTermTraductions = termRepository.findAllTraductionsOfConcept(idConcept, idTheso);
            for (NodeTermTraduction nodeTermTraduction : nodeTermTraductions) {
                JsonObjectBuilder jobLang = Json.createObjectBuilder();
                jobLang.add("lang", nodeTermTraduction.getLang());
                jobLang.add("title", nodeTermTraduction.getLexicalValue());
                jsonArrayBuilderLang.add(jobLang.build());
            }
            if (!nodeTermTraductions.isEmpty()) {
                job.add("labels", jsonArrayBuilderLang.build());
            }
            jsonArrayBuilder.add(job.build());
        }
        datasJson = jsonArrayBuilder.build().toString();

        if (datasJson != null) {
            return datasJson;
        } else {
            return null;
        }
    }

    /**
     * Pour retourner la liste des langues d'un thésaurus
     * https://pactols.frantiq.fr/opentheso/api/info/listlang?theso=TH_1&lang=all
     */
    @GetMapping(value = "info/listLang", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getlistLangOfTheso(@RequestParam(value = "theso") String idTheso) {

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(getlistLangOfTheso__(idTheso));
    }

    private String getlistLangOfTheso__(String idTheso) {

        var listLangOfTheso = thesaurusService.getAllUsedLanguagesOfThesaurus(idTheso);
        var jsonArrayBuilderLang = Json.createArrayBuilder();
        for (String idLang : listLangOfTheso) {
            JsonObjectBuilder jobLang = Json.createObjectBuilder();
            jobLang.add("lang", idLang);
            jsonArrayBuilderLang.add(jobLang.build());
        }
        return jsonArrayBuilderLang.build().toString();
    }

    // Pour retourner la dernière date de modification
    @GetMapping(value = "info/lastupdate", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getInfoLastUpdate(@RequestParam(value = "theso") String idTheso) {

        if (StringUtils.isEmpty(idTheso)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getInfoLastUpdate__(idTheso));
    }

    private String getInfoLastUpdate__(String idThesaurus) {
        var date = conceptService.getLastModification(idThesaurus);
        if (date == null) {
            return messageEmptyJson();
        }

        return "{\"lastUpdate\": \"" + date.toString() + "\"}";
    }

    /**
     * Pour retourner les concepts modifiés à partir de la date donnée
     * format de la date 2022-01-01
     */
    @GetMapping(value = "/getchangesfrom")
    public ResponseEntity<Object> getConceptsFrom(@RequestParam(value = "theso") String idTheso,
                                          @RequestParam(value = "date") String fromDate,
                                          @RequestParam(value = "format", required = false, defaultValue = "rdf") String format) {

        String formatOutput;
        switch (format) {
            case "rdf":
                format = CustomMediaType.APPLICATION_RDF;
                formatOutput = CustomMediaType.APPLICATION_RDF_UTF_8;
                break;
            case "jsonld":
                format = CustomMediaType.APPLICATION_JSON_LD;
                formatOutput = CustomMediaType.APPLICATION_JSON_LD_UTF_8;
                break;
            case "turtle":
                format = CustomMediaType.APPLICATION_TURTLE;
                formatOutput = CustomMediaType.APPLICATION_TURTLE_UTF_8;
                break;
            default:
                format = JSON_FORMAT;
                formatOutput = JSON_FORMAT_LONG;
        }
        var datas = restRDFHelper.getIdConceptFromDate(idTheso, fromDate, format);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(formatOutput)).body(datas);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////// Fonctions spécifiques pour Ontome  //////////////////////////////////////  
    ////////récupération des Toptermes qui sont liés à une classe CIDOC-CRM////////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * Pour retourner les concepts de la branche qui est liée à une classe CIDOC-CRM pour Ontome
     * le lien se fait par l'alignement en ExactMatch
     * Si la classe est renseignée, on retourne uniquement le concept en question
     * http://localhost:8082/opentheso2/api/ontome/linkedConcept?theso=th1&class=56
     * http://localhost:8082/opentheso2/api/ontome/linkedConcept?theso=th1
     */
    @GetMapping(value = "ontome/linkedConcept/", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getAllLinkedConceptsWithOntome(@RequestParam(value = "theso") String idTheso,
                                                         @RequestParam(value = "class", required = false) String cidocClass) {

        if (StringUtils.isEmpty(idTheso)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        String datas;
        if(StringUtils.isEmpty(cidocClass)) {
            datas = restRDFHelper.getAllLinkedConceptsWithOntome__(idTheso);
        } else {
            datas = restRDFHelper.getLinkedConceptWithOntome__(idTheso, cidocClass);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////// Fonctions qui  permettent de naviguer dans le thésaurus /////////////////  
    //////////////récupération des Toptermes et les fils à la demande//////////////////////
    ////////////// sert pour le widget à distance /////////////////////////////////////////    
    ///////////////////////////////////////////////////////////////////////////////////////

    //http://localhost:8082/opentheso2/api/topterm?theso=th19&lang=fr
    // permet de récupérer les TopTerms d'un thésaurus dans une langue donnée en précisant l'Id du thésaurus et la langue sont obligatoires
    @GetMapping(value = "/topterm", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getTopterms(@RequestParam(value = "theso") String idTheso,
                                      @RequestParam(value = "lang") String idLang) {

        if (StringUtils.isEmpty(idTheso) || StringUtils.isEmpty(idLang)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var datas = restRDFHelper.getTopTerms(idTheso, idLang);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    //http://localhost:8082/opentheso2/api/narrower?theso=th19&id=300&lang=fr
    /**
     * permet de récupérer les TopTerms d'un thésaurus dans une langue donnée
     * en précisant l'Id du thésaurus, id du concept et la langue sont obligatoires,
     */
    @GetMapping(value = "/narrower", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getNarrower(@RequestParam(value = "theso") String idTheso,
                                      @RequestParam(value = "id") String idConcept,
                                      @RequestParam(value = "lang") String idLang) {

        var datas = restRDFHelper.getNarrower(idTheso, idConcept, idLang);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    // Pour récupérer les informations sur un concept au format Json, les identifiants sont remplacés par les labels
    @GetMapping(value = "/{idTheso}.{idConcept}.labels", produces = JSON_FORMAT_LONG)
    public ResponseEntity<Object> getJsonFromIdConceptWithLabels(@PathVariable("idTheso") String idTheso,
                                                   @PathVariable("idConcept") String idConcept) {

        if (StringUtils.isEmpty(idTheso) || StringUtils.isEmpty(idConcept)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var datas = restRDFHelper.getInfosOfConcept(idTheso, idConcept, "fr");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }


    /**
     * Permet de retourner les Ids Ark fils pour un concept
     * 
     * /ark/allchilds?ark=un_ark 
     * http://localhost:8080/opentheso2/api/ark/allchilds?ark=67717/T124-66
     * {
        count: 10,
        arks: [ark1, ark2, ..., ark10]
       }
     */
    @GetMapping(value = "/ark/allchilds", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getIdArkOfConceptNT(@RequestParam(value = "ark") String idArk) {

        var datas = restRDFHelper.getChildsArkId(idArk);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////Fonction qui permet de produire /////////////////////////////////////////  
    //////////////des données Json pour le graphe D3js ////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////


    //http://localhost:8082/opentheso2/api/graph?theso=th19&id=266607&group=g12&lang=fr

    /**
     * permet de récupérer les données d'un thésaurus pour le graphe géré avec D3js
     * en précisant l'Id du thésaurus (obligatoire),
     * l'id du concept de départ,
     * l'id de la collection,
     * l'id de la langue
     */
    @GetMapping(value = "/graph", produces = CustomMediaType.APPLICATION_JSON_LD_UTF_8)
    public ResponseEntity<Object> getDatasForGraph(@RequestParam(value = "theso") String idTheso,
             @RequestParam(value = "id") String idConcept,
             @RequestParam(value = "lang") String idLang, @Parameter(name = "limit", example = "true", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), required = false, description = "pour limiter ou non le nombre de concepts à récupérer") @RequestParam(value = "limit", required = false, defaultValue = "false") Boolean limit)
    {

        var datas = d3jsHelper.findDatasForGraph__(idConcept, idTheso, idLang, limit);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

////////////////////////////////////////////////////////////////////////////////////
////////////////Permet de traduire les URI ARK par Opentheso////////////////////////
////////////////////////////////////////////////////////////////////////////////////

    // pour faire la redirection entre un IdArk et l'URL Opentheso
    @GetMapping(
            value = {
                    "/ark:/{naan}/{idArk}",   // ark:/naan/id
                    "/ark:{naan}/{idArk}"     // ark:naan/id
            },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUriFromArk(@PathVariable("naan") String naan,
                                           @PathVariable("idArk") String arkId) throws URISyntaxException {

        if (!org.springframework.util.StringUtils.hasText(naan) || !org.springframework.util.StringUtils.hasText(arkId)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(messageEmptyJson());
        }

        var webUrl = restRDFHelper.getUrlFromIdArk(naan, arkId);
        if (webUrl == null) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(getJsonMessage("Ark ID does not exist"));
        }

        return ResponseEntity.status(307) // 307 corresponds to temporary redirect
                .location(new URI(webUrl))
                .build();
    }

////////////////////////////////////////////////////////////////////////////////////
////////////////FIN traduire les URI ARK par Opentheso//////////////////////////////
////////////////////////////////////////////////////////////////////////////////////

    private String messageEmptyJson()
    {
        return "{}";
    }

    private String getJsonMessage(String message){
        var job = Json.createObjectBuilder();
        job.add("message", message);
        return job.build().toString();
    }
}
