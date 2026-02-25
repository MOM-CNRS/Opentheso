package fr.cnrs.opentheso.bean.importexport.newrdfimport;

import fr.cnrs.opentheso.models.skos.SkosConceptDto;

import fr.cnrs.opentheso.services.imports.rdf4j.newcode.RdfImportService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("skosRdfImportBean")
@SessionScoped
@Getter
@Setter
public class SkosRdfImportBean implements Serializable {

    private List<SkosConceptDto> concepts = new ArrayList<>();
    private transient org.primefaces.model.file.UploadedFile file;

    @Autowired
    private RdfImportService rdfImportService;

    public void handleFileUpload() {
        if (file == null) return;

        try (InputStream is = file.getInputStream()) {
            // Appel au service qui fait tout le parsing RDF
            concepts = rdfImportService.importRdf(is);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            concepts.size() + " concepts importés."));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
        }
    }

    // Getters / setters
    public List<SkosConceptDto> getConcepts() { return concepts; }
    public org.primefaces.model.file.UploadedFile getFile() { return file; }
    public void setFile(org.primefaces.model.file.UploadedFile file) { this.file = file; }
}
