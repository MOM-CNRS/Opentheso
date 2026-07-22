package fr.cnrs.opentheso.models.exports;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.services.ConceptService;

import jakarta.faces.context.FacesContext;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Data
@Service
public class UriHelper {

    private Preferences nodePreference;
    private String idTheso;
    @Autowired
    private ConceptService conceptService;


    /**
     * Cette fonction permet de retourner l'URI du thesaurus
     * Choix de l'URI pour l'export : 1 seule URI est possible pour l'export
     * 
     * @param idTheso
     * @param idArk
     * @param idHandle
     * @return 
     */
    public String getUriForTheso(String idTheso, String idArk, String idHandle) {
        String uri = "";
        if (idTheso == null || nodePreference == null) {
            return uri;
        }
        
        // type Ark
        if(nodePreference.isOriginalUriIsArk()) { 
            if (!StringUtils.isEmpty(idArk)) {
                uri = nodePreference.getOriginalUri()+ "/" + idArk;
                return uri;
            } else {
                uri = nodePreference.getCheminSite() + "?idt=" + idTheso;
                return uri;
            }
        }
        
        // type Handle
        if(nodePreference.isOriginalUriIsHandle()){
            if (!StringUtils.isEmpty(idHandle)) {
                uri = "https://hdl.handle.net/" + idHandle;
                return uri;
            }
        }

        // si on ne trouve pas ni Handle, ni Ark
        //http://localhost/opentheso2/?idt=th17
        if(!StringUtils.isEmpty(nodePreference.getOriginalUri())) {
            uri = nodePreference.getOriginalUri() + "/?idt=" + idTheso;
        } else {
            uri = getPath() + "/?idt=" + idTheso;
        }
        return uri;
    }      
    
    /**
     * Cette fonction permet de retourner l'URI du groupe
     * Choix de l'URI pour l'export : 1 seule URI est possible pour l'export par groupe
     * 
     * @param idGroup
     * @param idArk
     * @param idHandle
     * @return 
     */
    public String getUriForGroup(String idGroup, String idArk, String idHandle) {
        String uri = "";
        if (idTheso == null || nodePreference == null) {
            return uri;
        }
        
        // type Ark
        if(nodePreference.isOriginalUriIsArk()) { 
            if (!StringUtils.isEmpty(idArk)) {
                uri = nodePreference.getOriginalUri()+ "/" + idArk;
                return uri;
            }
        }
        
        // type Handle
        if(nodePreference.isOriginalUriIsHandle()){
            if (!StringUtils.isEmpty(idHandle)) {
                uri = "https://hdl.handle.net/" + idHandle;
                return uri;
            }
        }

        // si on ne trouve pas ni Handle, ni Ark
        if(!StringUtils.isEmpty(nodePreference.getOriginalUri())) {
            uri = nodePreference.getOriginalUri() + "/?idg=" + idGroup
                            + "&idt=" + idTheso;
        } else {
            uri = getPath() + "/?idg=" + idGroup
                            + "&idt=" + idTheso;
        }
        return uri;
    }    
    
    /**
     * Cette fonction permet de retourner l'URI du concept
     * Choix de l'URI pour l'export : 1 seule URI est possible pour l'export par concept 
     * @param idConcept
     * @param idArk
     * @param idHandle
     * @return 
     */
    public String getUriForConcept(String idConcept, String idArk, String idHandle) {
        String uri = "";
        if (idTheso == null || nodePreference == null) {
            return uri;
        }
        
        // type Ark
        if(nodePreference.isOriginalUriIsArk()) { 
            if (!StringUtils.isEmpty(idArk)) {
                uri = nodePreference.getOriginalUri()+ "/" + idArk;
                return uri;
            }
        }
        
        // type Handle
        if(nodePreference.isOriginalUriIsHandle()){
            if (!StringUtils.isEmpty(idHandle)) {
                uri = "https://hdl.handle.net/" + idHandle;
                return uri;
            }
        }

        // si on ne trouve pas ni Handle, ni Ark
        if(!StringUtils.isEmpty(nodePreference.getOriginalUri())) {
            uri = nodePreference.getOriginalUri() + "/?idc=" + idConcept
                            + "&idt=" + idTheso;
        } else {
            uri = getPath() + "/?idc=" + idConcept
                            + "&idt=" + idTheso;
        }
        return uri;
    }
    
    public String getIdArk(String idConcept) {
        if(nodePreference != null && nodePreference.isOriginalUriIsArk()){
            var concept = conceptService.getConcept(idConcept, idTheso);
            return concept != null ? concept.getIdArk() : null;
        }
        return null;
    }
    
    /**
     * permet de retourner le Path de l'application
     * exp:  //http://localhost:8082/opentheso2
     * @return
     */
    private String getPath(){
        if (nodePreference == null) {
            return "";
        }
        if(FacesContext.getCurrentInstance() == null) {
            if(StringUtils.isNotEmpty(nodePreference.getOriginalUri())){
                if(nodePreference.getOriginalUri().endsWith("/")) {
                    return nodePreference.getOriginalUri().substring(0, nodePreference.getOriginalUri().length() - 1);
                } else 
                    return nodePreference.getOriginalUri();
            } else if (StringUtils.isNotEmpty(nodePreference.getCheminSite())) {
                if(nodePreference.getCheminSite().endsWith("/")) {
                    return nodePreference.getCheminSite().substring(0, nodePreference.getCheminSite().length() - 1);
                } else 
                    return nodePreference.getCheminSite();
            } else {
                return "";
            }
        }
        String path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }    
    
}
