package fr.cnrs.opentheso.client.alignement;

import fr.cnrs.opentheso.client.CurlHelper;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.MalformedQueryException;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.utils.JsonHelper;
import lombok.Data;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import org.eclipse.rdf4j.repository.RepositoryConnection;


@Data
public class WikidataHelper {

    private StringBuffer messages = new StringBuffer();
    // les informations récupérées de Wikidata
    private List<SelectedResource> resourceWikidataTraductions;
    private List<SelectedResource> resourceWikidataDefinitions;
    private List<SelectedResource> resourceWikidataImages;


    public ArrayList<NodeAlignment> queryWikidata_rest(String idC, String idTheso, String lexicalValue, String lang,
            String query, String source) {
        
        if (query.trim().equals("") ) {
            return null;
        }

        if (lexicalValue.trim().equals("")) {
            return null;
        }        

        if (query.trim().equals("") ) {
            return null;
        }

        ArrayList<NodeAlignment> listeAlign = null;

        try {
            // Encodage du terme
            String encodedValue = URLEncoder.encode(lexicalValue, StandardCharsets.UTF_8);

            // Remplacement dans la query
            String finalQuery = query
                    .replace("##value##", encodedValue)
                    .replace("##lang##", lang);

            // Création du client
            HttpClient client = HttpClient.newHttpClient();

            // Requête GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalQuery))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Opentheso/1.0") // important !
                    .GET()
                    .build();

            // Envoi + récupération de la réponse
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 202) {
                String json = response.body();
                listeAlign = getValuesFromJson(json, idC, idTheso, source);
            } else {
                System.err.println("Erreur HTTP : " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return listeAlign;


//        try {
//            lexicalValue = URLEncoder.encode(lexicalValue, "UTF-8");
//            lexicalValue = lexicalValue.replaceAll(" ", "%20");
//            query = query.replaceAll("##value##", lexicalValue);
//            query = query.replaceAll("##lang##", lang);
//            query = query + "&origin=*";
//            URL url = new URL(query);
//
//            var cons = (HttpsURLConnection) url.openConnection();
//            cons.setRequestMethod("GET");
//            cons.setRequestProperty("Accept", "application/json");
//
//            if (cons.getResponseCode() != 200){
//                if (cons.getResponseCode() != 202) {
//                    return null;
//                }
//            }
//
//            var br = new BufferedReader(new InputStreamReader((cons.getInputStream())));
//            String output;
//            String xmlRecord = "";
//            while ((output = br.readLine()) != null) {
//                xmlRecord += output;
//            }
//            cons.disconnect();
//            br.close();
//            listeAlign = getValuesFromJson(xmlRecord, idC, idTheso, source);
//        } catch (Exception e) {}
//
//        return listeAlign;
     }
    
    private ArrayList<NodeAlignment> getValuesFromJson(String jsonValue, String idConcept, String idTheso, String source){
        JsonObject object;
        JsonArray jsonArray;
        JsonObject value;
        String localValue;
       
        ArrayList<NodeAlignment> listAlignValues = new ArrayList<>();
        
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(jsonValue));
            object = jsonReader.readObject();
            localValue = object.getJsonObject("searchinfo").getString("search");
            jsonArray = object.getJsonArray("search");

            for (int i = 0; i < jsonArray.size(); i++) {
                NodeAlignment na = new NodeAlignment();
                na.setInternal_id_concept(idConcept);
                na.setInternal_id_thesaurus(idTheso);
                na.setLabelLocal(localValue);
                value = jsonArray.getJsonObject(i);
                // label ou Nom
                try {
                     na.setConcept_target(value.getJsonObject("match").getString("text"));
                } catch (Exception e) {
                    continue;
                }                    

                // description
                try {
                    if(value.getString("description") != null) {
                        na.setDef_target(value.getString("description"));
                    } else {
                        na.setDef_target("");
                    }
                } catch (Exception e) {
                    na.setDef_target("");
                }                

                na.setThesaurus_target(source);

                // URI
                try {
                    na.setUri_target(value.getString("concepturi"));
                    na.setUri_target(na.getUri_target().replaceAll("http://", "https://"));
                } catch (Exception e) {
                    continue;
                }                  

                listAlignValues.add(na);                
            }            
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return listAlignValues;
    }
  
    /**
     * Alignement du thésaurus vers la source Wikidata en Sparql et en retour du
     * Json
     *
     * @param idC
     * @param idTheso
     * @param requete
     * @param source
     * @return
     */
    
    public List<NodeAlignment> queryWikidata_sparql(String idC, String idTheso, String requete, String source) {
        List<NodeAlignment> listAlignValues = new ArrayList<>();

        String sparqlEndpoint = "https://query.wikidata.org/sparql";
        SPARQLRepository repo = new SPARQLRepository(sparqlEndpoint);

        String userAgent = "opentheso";
        repo.setAdditionalHttpHeaders( Collections.singletonMap("User-Agent", userAgent ) );
        
        
        List<Map<String, String>> resultsList = new ArrayList<>();

        try (RepositoryConnection conn = repo.getConnection()) {
            TupleQuery query = conn.prepareTupleQuery(requete);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Map<String, String> resultMap = new HashMap<>();

                    for (String bindingName : bindingSet.getBindingNames()) {
                        Value value = bindingSet.getValue(bindingName);
                        if (value != null) {
                            resultMap.put(bindingName, value.stringValue());
                        }
                    }

                    resultsList.add(resultMap);
                }
            } catch (QueryEvaluationException | RepositoryException ex) {
                ex.printStackTrace();
            }
            
        } catch (RepositoryException | MalformedQueryException ex) {
            ex.printStackTrace();
        }        
        for (Map<String, String> result : resultsList) {
            NodeAlignment na = new NodeAlignment();
            na.setInternal_id_concept(idC);
            na.setInternal_id_thesaurus(idTheso);    

            // label ou Nom
            if (result.get("itemLabel") != null) {
                na.setConcept_target(result.get("itemLabel"));
            } else {
                continue;
            }       
            // description
            if (result.get("itemDescription") != null) {
                na.setDef_target(result.get("itemDescription"));
            } else {
                na.setDef_target("");
            }     
            na.setThesaurus_target(source);            
            // URI
            if (result.get("item") != null) {
                na.setUri_target(result.get("item"));
            } else {
                continue;
            }
            listAlignValues.add(na);            
        }         
        return listAlignValues;
    }
    

    /**
     * Cette fonction permet de récupérer les options de Wikidata Images,
     * alignements, traductions....ource
     *
     * @param selectedNodeAlignment
     * @param selectedOptions
     * @param thesaurusUsedLanguageWithoutCurrentLang
     * @param thesaurusUsedLanguage
     */
    public void setOptionsFromWikidata(NodeAlignment selectedNodeAlignment, List<String> selectedOptions,
            List<String> thesaurusUsedLanguageWithoutCurrentLang, List<String> thesaurusUsedLanguage) {
        if (selectedNodeAlignment == null) {
            return;
        }
        CurlHelper curlHelper = new CurlHelper();
        curlHelper.setHeader1("Accept");
        curlHelper.setHeader2("application/json");

        String uri = selectedNodeAlignment.getUri_target();//."https://www.wikidata.org/entity/Q178401";//"https://www.wikidata.org/entity/Q178401";//Q7748";Q324926
        String datas = curlHelper.getDatasFromUriHttps(uri);
        String entity = uri.substring(uri.lastIndexOf("/") + 1);

        for (String selectedOption : selectedOptions) {
            switch (selectedOption) {
                case "langues":
                    resourceWikidataTraductions = getTraductions(datas, entity, thesaurusUsedLanguageWithoutCurrentLang);
                    break;
                case "notes":
                    resourceWikidataDefinitions = getDescriptions(datas, entity, thesaurusUsedLanguage);
                    break;
                case "images":
                    resourceWikidataImages = getImages(datas, entity);
                    break;
            }
        }
    }

    /**
     * récupération des traductions
     *
     * @param jsonDatas
     * @param entity
     * @param languages
     * @return
     */
    private List<SelectedResource> getTraductions(String jsonDatas, String entity, List<String> languages) {
        List<SelectedResource> traductions = new ArrayList<>();
        if (StringUtils.isEmpty(jsonDatas)) {
            return traductions;
        }
        JsonHelper jsonHelper = new JsonHelper();
        JsonObject jsonObject = jsonHelper.getJsonObject(jsonDatas);
        JsonObject jsonObject1;
        JsonValue jsonValue;

        String lang;
        String value;

        try {
            jsonObject1 = jsonObject.getJsonObject("entities").getJsonObject(entity).getJsonObject("labels");
        } catch (Exception e) {
            return null;
        }
        for (String language : languages) {
            try {
                SelectedResource selectedResource = new SelectedResource();
                jsonValue = jsonObject1.getJsonObject(language).get("language");
                lang = jsonValue.toString().replace("\"", "");
                selectedResource.setIdLang(lang);
                jsonValue = jsonObject1.getJsonObject(language).get("value");
                value = jsonValue.toString().replace("\"", "");
                selectedResource.setGettedValue(value);
                traductions.add(selectedResource);
            } catch (Exception e) {
            }
        }
        return traductions;
    }

    /**
     * permet de récupérer les descriptions de wikidata
     *
     * @param jsonDatas
     * @param entity
     * @param languages
     * @return
     */
    private List<SelectedResource> getDescriptions(String jsonDatas, String entity, List<String> languages) {

        List<SelectedResource> descriptions = new ArrayList<>();

        if (StringUtils.isEmpty(jsonDatas)) {
            return descriptions;
        }

        JsonObject jsonObject = new JsonHelper().getJsonObject(jsonDatas);

        JsonObject jsonObject1 = jsonObject.getJsonObject("entities").getJsonObject(entity).getJsonObject("descriptions");
        for (String language : languages) {
            try {
                SelectedResource selectedResource = new SelectedResource();
                JsonValue jsonValue = jsonObject1.getJsonObject(language).get("language");
                selectedResource.setIdLang(jsonValue.toString().replace("\"", ""));
                jsonValue = jsonObject1.getJsonObject(language).get("value");
                selectedResource.setGettedValue(jsonValue.toString().replace("\"", ""));
                descriptions.add(selectedResource);
            } catch (Exception e) {
            }
        }

        return descriptions;
    }

    /**
     * permet de récupérer les images de Wikidata
     *
     * @param jsonDatas
     * @param entity
     * @return
     */
    private List<SelectedResource> getImages(String jsonDatas, String entity) {
        // pour construire l'URL de Wikimedia, il faut ajouter 
        // http://commons.wikimedia.org/wiki/Special:FilePath/
        // puis le nom de l'image

        String fixedUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/";

        if (StringUtils.isEmpty(jsonDatas)) {
            return Collections.emptyList();
        }

        JsonHelper jsonHelper = new JsonHelper();
        JsonObject jsonObject = jsonHelper.getJsonObject(jsonDatas);
        JsonObject jsonObject1;
        JsonObject jsonObject2;
        JsonValue jsonValue;

        ArrayList<SelectedResource> imagesUrls = new ArrayList<>();

        try {
            jsonObject1 = jsonObject.getJsonObject("entities").getJsonObject(entity).getJsonObject("claims");//.getJsonObject("P18");
        } catch (Exception e) {
            return null;
        }

        try {
            JsonArray jsonArray = jsonObject1.getJsonArray("P18");
            for (int i = 0; i < jsonArray.size(); i++) {
                SelectedResource selectedResource = new SelectedResource();
                jsonObject2 = jsonArray.getJsonObject(i);
                jsonValue = jsonObject2.getJsonObject("mainsnak").getJsonObject("datavalue").get("value");
                selectedResource.setGettedValue(fixedUrl + jsonValue.toString().replace("\"", ""));
                imagesUrls.add(selectedResource);
            }

        } catch (Exception e) {
        }
        return imagesUrls;
    }

}
