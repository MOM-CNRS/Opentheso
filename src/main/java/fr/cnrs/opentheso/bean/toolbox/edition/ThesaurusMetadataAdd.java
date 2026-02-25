package fr.cnrs.opentheso.bean.toolbox.edition;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.RowEditEvent;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "thesaurusMetadataAdd")
public class ThesaurusMetadataAdd implements Serializable{

    private final ThesaurusDcTermRepository thesaurusDcTermRepository;

    private List<DcElement> dcElements;
    private List<String> dcmiResource, dcmiTypes;
    private String idTheso;


    public void init(String idTheso1) {

        dcElements = getThesaurusMetadata(idTheso1);
        dcmiResource = new DCMIResource().getAllResources();
        dcmiTypes = new DCMIResource().getAllTypes();        
        this.idTheso = idTheso1;
    }
    
    /**
     * retour des méta-données à la demande
     */
    public List<DcElement> getThesaurusMetadata(String idThesaurus){
        var tmp = thesaurusDcTermRepository.findAllByIdThesaurus(idThesaurus);
        if (CollectionUtils.isNotEmpty(tmp)) {
            return tmp.stream().map(element -> DcElement.builder()
                            .id(element.getId().intValue())
                            .name(element.getName())
                            .value(element.getValue())
                            .language(element.getLanguage())
                            .type(element.getDataType())
                            .build())
                    .collect(Collectors.toCollection(ArrayList::new)); // <- ici
        } else {
            return new ArrayList<>();
        }
    }
    
    public void initlanguage(DcElement dcElement){
        if(!StringUtils.isEmpty(dcElement.getType()))
            dcElement.setLanguage("");
    }

    public void initType(DcElement dcElement){
        if(!StringUtils.isEmpty(dcElement.getLanguage()))
            dcElement.setType("");
    }

    public void deleteThesaurusMetadata(DcElement dcElement){
        thesaurusDcTermRepository.deleteDcElementThesaurus(dcElement.getId(), idTheso);
        init(idTheso);
        FacesMessage msg = new FacesMessage("Dcterms Supprimé", "");
        FacesContext.getCurrentInstance().addMessage(null, msg);        
    }
    
    public void onRowEdit(RowEditEvent<DcElement> event) {
        DcElement dcElementTemp = event.getObject();
        if(StringUtils.isEmpty(dcElementTemp.getValue())){
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Value required", "" + dcElementTemp.getId());
            FacesContext.getCurrentInstance().addMessage(null, msg);         
            return;
        }

        if(dcElementTemp.getId() == -1) {
            try {
                var tmp = thesaurusDcTermRepository.save(ThesaurusDcTerm.builder()
                        .idThesaurus(idTheso)
                        .name(dcElementTemp.getName())
                        .value(dcElementTemp.getValue())
                        .language(dcElementTemp.getLanguage())
                        .dataType(dcElementTemp.getType())
                        .build());
                dcElementTemp.setId(tmp.getId().intValue());
            }
            catch (DataIntegrityViolationException e) {
                log.debug("DC Term déjà existant, insertion ignorée : {} {} {}",
                        idTheso, dcElementTemp.getName(), dcElementTemp.getValue());
            }

        } else {
            var tmp = thesaurusDcTermRepository.findById(dcElementTemp.getId());
            if (tmp.isPresent()) {
                tmp.get().setName(dcElementTemp.getName());
                tmp.get().setLanguage(dcElementTemp.getLanguage());
                tmp.get().setValue(dcElementTemp.getValue());
                tmp.get().setDataType(dcElementTemp.getType());
                try {
                    thesaurusDcTermRepository.save(tmp.get());
                } catch (DataIntegrityViolationException e) {
                    log.debug("DC Term déjà existant, insertion ignorée : {} {} {}",
                            idTheso, dcElementTemp.getName(), dcElementTemp.getValue());
                }
            }
        } 
        FacesMessage msg = new FacesMessage("Dcterms updated", dcElementTemp.getValue());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void onAddNew() {
        DcElement dcElement = new DcElement("", "", "","");
        dcElements.add(dcElement);
    }
}
