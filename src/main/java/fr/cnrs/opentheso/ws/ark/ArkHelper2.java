package fr.cnrs.opentheso.ws.ark;

import java.util.Properties;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.utils.StringUtils;
import java.util.ArrayList;


public class ArkHelper2 {
    
    private Preferences nodePreference;
    private String message;
    
    private String idArk;
    private String idHandle;
    private ArkClientRest arkClientRest;
    
    public ArkHelper2(Preferences nodePreference) {
        this.nodePreference = nodePreference;
    }
    
    public boolean login() {
        arkClientRest = new ArkClientRest();

        if (nodePreference == null) {
            message = "Pas de préférences pour le thésaurus";
            return false;
        }

        String serverHost = nodePreference.getServerArk();
        String idNaan = nodePreference.getIdNaan();
        String user = nodePreference.getUserArk();
        String password = nodePreference.getPassArk();

        if (isBlank(serverHost) || isBlank(idNaan) || isBlank(user) || isBlank(password)) {
            message = "Paramètres Ark incomplets (URL serveur, NAAN, utilisateur et mot de passe sont obligatoires)";
            return false;
        }

        Properties propertiesArk = new Properties();
        propertiesArk.setProperty("serverHost", serverHost);
        propertiesArk.setProperty("idNaan", idNaan);
        propertiesArk.setProperty("user", user);
        propertiesArk.setProperty("password", password);
        arkClientRest.setPropertiesArk(propertiesArk);
        try {
            boolean ok = arkClientRest.login();
            if (!ok) {
                message = "Échec de la connexion au serveur Ark";
            }
            return ok;
        } catch (Exception e) {
            message = "Erreur de connexion au serveur Ark: " + e.getMessage();
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    public boolean isArkExistOnServer(String idArk) {
        return  arkClientRest.isArkExist(idArk);
    }
    
    
    public boolean isHandleExistOnServer(String idHandle) {
        return arkClientRest.isHandleExist(idHandle);
    }
    
    /**
     * Permet de créer d'ajouter un identifiant ARK sur le serveur 
     * en pamaramètre l'Id Ark qu'on souhaite créer + identifiant Handle (serveur MOM)
     * @param idArkProvided
     * @param privateUri
     * @param nodeMetaData
     * @return
     */
    public boolean addArkWithProvidedId(
            String idArkProvided,
            String privateUri,
            NodeMetaData nodeMetaData) {
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
        idHandle = null;
        
        NodeJson2 nodeJson2 = new NodeJson2();
        nodeJson2.setArk(idArkProvided);
        nodeJson2.setUrlTarget(nodePreference.getCheminSite() + privateUri); //"?idc=" + idConcept + "&idt=" + idThesaurus);
        nodeJson2.setTitle(nodeMetaData.getTitle());
        nodeJson2.setCreator(nodeMetaData.getCreator());
        nodeJson2.setDcElements(nodeMetaData.getDcElementsList()); 
        nodeJson2.setType(nodePreference.getPrefixArk());  //pcrt : p= pactols, crt=code DCMI pour collection
        nodeJson2.setQualifiers(new ArrayList<>()); 
        nodeJson2.setNaan(nodePreference.getIdNaan());
        nodeJson2.setUseHandle(nodePreference.isGenerateHandle());
       
        // récupération du Token
        if(arkClientRest.getLoginJson() == null) return false;

        String token = arkClientRest.getToken();
        nodeJson2.setToken(token);
       
        // création de l'identifiant Ark et Handle 
        String jsonDatas = nodeJson2.getJsonString();
        if(jsonDatas == null) return false;
        if(!arkClientRest.addArk(jsonDatas)) return false;        
        
        idArk = arkClientRest.getIdArk();
        idHandle = arkClientRest.getIdHandle();
        if (idArk == null) {
            message = "La connexion Ark a échouée";
            return false;
        }
        if (idHandle == null) {
            message = "La connexion Handle a échouée";
            return false;
        }
        return true;
    }    
    
    
    public boolean getArkFromArkeo(String ark){
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
        idArk = null;
        idHandle = null;
        
        if(!arkClientRest.getArk(ark)) return false;

        idArk = arkClientRest.getIdArk();
        idHandle = arkClientRest.getIdHandle();
        if (idArk == null) {
            message = "Erreur Ark !!";
            return false;
        }
        if (idHandle == null) {
            message = "Erreur Handle !!";
            return false;
        }
        return true;        
    }
    
    
    /**
     * Permet de créer un identifiant ARK et un identifiant Handle (serveur MOM)
     * @param jsonDatas
     * @return
     */
    public String addBatchArk(String jsonDatas) {
        if (nodePreference == null) {
            return null;
        }
        if (!nodePreference.isUseArk()) {
            return null;
        }
        
        return arkClientRest.addBatchArk2(jsonDatas);

    }    
    
    /**
     * Permet de créer un identifiant ARK et un identifiant Handle (serveur MOM)
     * @param privateUri
     * @param nodeMetaData
     * @return
     */
    public boolean addArk(String privateUri,
            NodeMetaData nodeMetaData) {
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
        idArk = "";
        idHandle = "";
      
        NodeJson2 nodeJson2 = new NodeJson2();
        nodeJson2.setUrlTarget(nodePreference.getCheminSite() + privateUri); //"?idc=" + idConcept + "&idt=" + idThesaurus);
        nodeJson2.setTitle(nodeMetaData.getTitle());
        nodeJson2.setCreator(nodeMetaData.getCreator());
        nodeJson2.setDcElements(nodeMetaData.getDcElementsList()); 
        nodeJson2.setType(nodePreference.getPrefixArk());  //pcrt : préfixe pour sous distinguer une sous entité:  p= pactols, crt= code DCMI pour collection
        nodeJson2.setNaan(nodePreference.getIdNaan());
        nodeJson2.setArk(idArk);
        nodeJson2.setUseHandle(nodePreference.isGenerateHandle());
        
        // récupération du Token
        if(arkClientRest.getLoginJson() == null) return false;
    
        String token = arkClientRest.getToken();
        nodeJson2.setToken(token);

        // création de l'identifiant Ark et Handle 
        String jsonDatas = nodeJson2.getJsonString();
        if(jsonDatas == null) return false;
        if(!arkClientRest.addArk(jsonDatas)){
            message = message + arkClientRest.getMessage();
            return false;
        }

        idArk = arkClientRest.getIdArk();
        idHandle = arkClientRest.getIdHandle();
        if (idArk == null) {
            message = "La connexion Ark a échouée";
            return false;
        }
        if(nodePreference.isGenerateHandle()) {
            if (idHandle == null) {
                message = "La connexion Handle a échouée";
                return false;
            }
        }
        return true;
    }
    
    /**
     * Permet de mettre à jour un objet ARK suivant l'Id Ark (serveur MOM)
     * @param idArk
     * @param privateUri
     * @param nodeMetaData
     * @return
     */
    public boolean updateArk(
            String idArk,
            String privateUri,
            NodeMetaData nodeMetaData) {
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
        idHandle = null;
     
        NodeJson2 nodeJson2 = new NodeJson2();
        nodeJson2.setUrlTarget(nodePreference.getCheminSite() + privateUri); //"?idc=" + idConcept + "&idt=" + idThesaurus);
        nodeJson2.setArk(idArk);
        nodeJson2.setTitle(StringUtils.convertString(nodeMetaData.getTitle()));
        nodeJson2.setCreator(nodeMetaData.getCreator());
        nodeJson2.setDcElements(nodeMetaData.getDcElementsList()); // n'est pas encore exploité
        nodeJson2.setType(nodePreference.getPrefixArk());  //pcrt : p= pactols, crt=code DCMI pour collection
        nodeJson2.setNaan(nodePreference.getIdNaan());
        nodeJson2.setUseHandle(nodePreference.isGenerateHandle());
       
        String token = arkClientRest.getToken();
        nodeJson2.setToken(token);        

        // mise à jour de Ark et Handle 
        String jsonDatas = nodeJson2.getJsonString();
        if(jsonDatas == null) return false;        
        
        if(!arkClientRest.updateArk(jsonDatas)) {
            message = message + arkClientRest.getMessage();
            return false;
        }

        idHandle = arkClientRest.getIdHandle();

        if(nodePreference.isGenerateHandle()) {        
            if (idHandle == null) {
                message = "La connexion Handle a échouée";
                return false;
            }
        }
        return true;
    }    
    
    /**
     * Permet de mettre à jour l'URI de l'identifiant ARK suivant l'Id Ark (serveur MOM)
     * @param idArk
     * @param privateUri
     * @return
     */
    public boolean updateUriArk(
            String idArk,
            String privateUri) {
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
     
        NodeJson2 nodeJson2 = new NodeJson2();
        nodeJson2.setUrlTarget(nodePreference.getCheminSite() + privateUri); //"?idc=" + idConcept + "&idt=" + idThesaurus);
        nodeJson2.setArk(idArk);
        nodeJson2.setNaan(nodePreference.getIdNaan());
        nodeJson2.setUseHandle(nodePreference.isGenerateHandle());
        String token = arkClientRest.getToken();
        nodeJson2.setToken(token);        

        String jsonDatas = nodeJson2.getJsonUriString();
        if(jsonDatas == null) return false;        
        
        if(!arkClientRest.updateUriArk(nodeJson2.getJsonString())) {
            message = message + arkClientRest.getMessage();
            return false;
        }
        return true;
    }       
    
    public String getToken() {
        return arkClientRest.getToken();
    }
            
    
    
    
    
    
 ////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////// à modifier //////////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////    
    
    
    
    
    /**
     * à basculer vers la nouvelle version du Webservices Ark
     * Permet de supprimer un Handle en local et sur handle.net
     * 
     * @param idHandle
     * @param privateUri
     * @param nodeMetaData
     * @return
     */
    public boolean deleteHandle(
            String idHandle,
            String privateUri,
            NodeMetaData nodeMetaData) {
        if (nodePreference == null) {
            return false;
        }
        if (!nodePreference.isUseArk()) {
            return false;
        }
        
        NodeJson nodeJson = new NodeJson();
        nodeJson.setArk("");
        
        /// c'est le seul argument qui compte pour la suppression 
        nodeJson.setHandle(idHandle);
        
        
        nodeJson.setUrlTarget(nodePreference.getCheminSite() + privateUri); //"?idc=" + idConcept + "&idt=" + idThesaurus);
        nodeJson.setTitle(nodeMetaData.getTitle());
        nodeJson.setCreator(nodeMetaData.getCreator());
        nodeJson.setDcElements(nodeMetaData.getDcElementsList()); // n'est pas encore exploité
        nodeJson.setType(nodePreference.getPrefixArk());  //pcrt : p= pactols, crt=code DCMI pour collection
        nodeJson.setLanguage(nodePreference.getSourceLang());
        nodeJson.setNaan(nodePreference.getIdNaan());
        nodeJson.setHandle_prefix("20.500.11859");
       
        // création de l'identifiant Ark et Handle 
        if(!arkClientRest.deleteHandle(nodeJson.getJsonString())) {
            message = arkClientRest.getMessage();
            return false;
        }
        message = arkClientRest.getMessage();
        return true;
    }        
    
    
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIdArk() {
        return idArk;
    }

    public void setIdArk(String idArk) {
        this.idArk = idArk;
    }

    public String getIdHandle() {
        return idHandle;
    }

    public void setIdHandle(String idHandle) {
        this.idHandle = idHandle;
    }
    
    
    
}
