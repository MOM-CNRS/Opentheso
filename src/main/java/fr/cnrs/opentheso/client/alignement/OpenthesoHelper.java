package fr.cnrs.opentheso.client.alignement;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.services.imports.rdf4j.ReadRDF4JNewGen;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 *
 * @author miled.rousset
 */
@Data
public class OpenthesoHelper {

    private StringBuffer messages = new StringBuffer();

    // les informations récupérées de Wikidata
    private ArrayList<SelectedResource> resourceOpenthesoTraductions;
    private ArrayList<SelectedResource> resourceOpenthesoDefinitions;
    private ArrayList<SelectedResource> resourceOpenthesoImages;

    //Alignement du thésaurus vers la source Wikidata en Sparql et en retour du Json
    public ArrayList<NodeAlignment> queryOpentheso(String idC, String idTheso,
            String lexicalValue, String idLang,
            String query, String source) {

        if (query.trim().equals("") ) {
            return null;
        }
        if (lexicalValue.trim().equals("")) {
            return null;
        }        
        String originalLabel = new String(lexicalValue);

        ArrayList<NodeAlignment> listeAlign = new ArrayList<>();
        // construction de la requête de type (webservices Opentheso)
        HttpsURLConnection cons = null;
        HttpURLConnection con = null;
        BufferedReader br;
        try {
            lexicalValue = URLEncoder.encode(lexicalValue, "UTF-8");
            lexicalValue = lexicalValue.replaceAll(" ", "%20");
            query = query.replace("##lang##", idLang);
            query = query.replace("##value##", lexicalValue);       
            URL url = new URL(query);
            if(query.startsWith("https://")) {
                cons = (HttpsURLConnection) url.openConnection();
                cons.setRequestMethod("GET");
                cons.setRequestProperty("Accept", "application/rdf+xml");
                if (cons.getResponseCode() != 200){
                    if (cons.getResponseCode() != 202) {
                        messages.append(cons.getResponseMessage());
                        return null;
                    }
                }
                br = new BufferedReader(new InputStreamReader((cons.getInputStream())));                
            }
            else {
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/rdf+xml");
                if (con.getResponseCode() != 200){
                    if (con.getResponseCode() != 202) {
                        messages.append(con.getResponseMessage());
                        return null;
                    }
                }
                br = new BufferedReader(new InputStreamReader((con.getInputStream())));                  
            }

            String output;
            String xmlRecord = "";
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }
            if(cons != null)
                cons.disconnect();
            if(con != null)
                con.disconnect();
            
            listeAlign = getValues(originalLabel, xmlRecord, idC, idLang, idTheso, source);
            br.close();
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return listeAlign;
    }

    private ArrayList<NodeAlignment> getValues(String originalValue, String xmlDatas,
            String idC, String idLang, String idTheso, String source) {

        ArrayList<NodeAlignment> listAlignValues = new ArrayList<>();
        
        InputStream inputStream;
        SKOSXmlDocument sxd;
        try {
            inputStream = new ByteArrayInputStream(xmlDatas.getBytes("UTF-8"));
            sxd = new ReadRDF4JNewGen().readRdfFlux(inputStream, RDFFormat.RDFXML, idLang, new StringBuffer());

            for (SKOSResource resource : sxd.getConceptList()) {
                NodeAlignment na = new NodeAlignment();
                na.setInternal_id_concept(idC);
                na.setLabelLocal(originalValue);
                na.setInternal_id_thesaurus(idTheso);
                na.setThesaurus_target(source);//"Pactols");
                na.setUri_target(resource.getUri());
                for(SKOSLabel label : resource.getLabelsList()) {
                    switch (label.getProperty()) {
                        case SKOSProperty.PREF_LABEL:
                            if(label.getLanguage().equals(idLang)) {
                                na.setConcept_target(label.getLabel());
                            }
                            break;
                        case SKOSProperty.ALT_LABEL:
                            if(label.getLanguage().equals(idLang)) {
                                if(StringUtils.isEmpty(na.getConcept_target_alt())) {
                                    na.setConcept_target_alt(label.getLabel());
                                } else {
                                    na.setConcept_target_alt(na.getConcept_target_alt() + ";" + label.getLabel());
                                }
                            }
                            break;                                
                        default:
                            break;
                    }
                }

                for(SKOSDocumentation sd : resource.getDocumentationsList()) {
                    if(sd.getProperty() == SKOSProperty.DEFINITION && sd.getLanguage().equals(idLang)) {
                        na.setDef_target(sd.getText());
                    }
                }
                listAlignValues.add(na);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(OpenthesoHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {            
            Logger.getLogger(OpenthesoHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return listAlignValues;
    }

    public String getMessages() {
        return messages.toString();
    }
}
