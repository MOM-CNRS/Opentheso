package fr.cnrs.opentheso.services.imports.rdf4j;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.imports.AddConceptsStruct;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.imports.ThesaurusSkosImportEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @deprecated Use {@link ThesaurusSkosImportEngine} directly.
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class ImportRdf4jHelper {

    private final ThesaurusSkosImportEngine thesaurusSkosImportEngine;

    public void setInfos(String formatDate, int idUser, int idGroupUser, String langueSource) {
        thesaurusSkosImportEngine.setInfos(formatDate, idUser, idGroupUser, langueSource);
    }

    public void setSelectedIdentifier(String selectedIdentifier) {
        thesaurusSkosImportEngine.setSelectedIdentifier(selectedIdentifier);
    }

    public void setPrefixHandle(String prefixHandle) {
        thesaurusSkosImportEngine.setPrefixHandle(prefixHandle);
    }

    public void setPersistentName(String persistentName) {
        thesaurusSkosImportEngine.setPersistentName(persistentName);
    }

    public void setPrefixDoi(String prefixDoi) {
        thesaurusSkosImportEngine.setPrefixDoi(prefixDoi);
    }

    public void setNodePreference(Preferences nodePreference) {
        thesaurusSkosImportEngine.setNodePreference(nodePreference);
    }

    public String addThesaurus() throws SQLException {
        return thesaurusSkosImportEngine.addThesaurus();
    }

    public void addFacets(ArrayList<SKOSResource> facetResources, String idTheso) {
        thesaurusSkosImportEngine.addFacets(facetResources, idTheso);
    }

    public void addGroups(ArrayList<SKOSResource> groupResource, String idTheso) {
        thesaurusSkosImportEngine.addGroups(groupResource, idTheso);
    }

    public void addConcept(SKOSResource conceptResource, String idTheso, boolean isCandidatImport) {
        thesaurusSkosImportEngine.addConcept(conceptResource, idTheso, isCandidatImport);
    }

    public void addConceptV2(SKOSResource conceptResource, String idTheso) {
        thesaurusSkosImportEngine.addConceptV2(conceptResource, idTheso);
    }

    public void addFoafImages(ArrayList<SKOSResource> foafImages, String idTheso) {
        thesaurusSkosImportEngine.addFoafImages(foafImages, idTheso);
    }

    public void addFacetsV2(ArrayList<SKOSResource> facetResources, String idTheso) {
        thesaurusSkosImportEngine.addFacetsV2(facetResources, idTheso);
    }

    public void initAddConceptsStruct(
            AddConceptsStruct acs,
            SKOSResource conceptResource,
            String idTheso,
            boolean isCandidatImport
    ) {
        thesaurusSkosImportEngine.initAddConceptsStruct(acs, conceptResource, idTheso, isCandidatImport);
    }

    public void addConceptToBdd(AddConceptsStruct acs, String idThesaurus, boolean isCandidatImport) {
        thesaurusSkosImportEngine.addConceptToBdd(acs, idThesaurus, isCandidatImport);
    }

    public void addRelation(AddConceptsStruct acs, String idTheso) {
        thesaurusSkosImportEngine.addRelation(acs, idTheso);
    }

    public void addLangsToThesaurus(String idTheso) {
        thesaurusSkosImportEngine.addLangsToThesaurus(idTheso);
    }

    public SKOSXmlDocument getRdf4jThesaurus() {
        return thesaurusSkosImportEngine.getRdf4jThesaurus();
    }

    public void setRdf4jThesaurus(SKOSXmlDocument rdf4jThesaurus) {
        thesaurusSkosImportEngine.setRdf4jThesaurus(rdf4jThesaurus);
    }

    public StringBuilder getMessage() {
        return thesaurusSkosImportEngine.getMessage();
    }
}
