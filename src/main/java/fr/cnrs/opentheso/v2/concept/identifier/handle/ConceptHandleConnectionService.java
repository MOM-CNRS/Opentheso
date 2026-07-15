package fr.cnrs.opentheso.v2.concept.identifier.handle;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.List;

import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.handle.HandleClient;
import fr.cnrs.opentheso.entites.Preferences;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptHandleConnectionService {

    @Value("${certificats.admpriv}")
    private String admprivPath;

    @Value("${certificats.cacerts2}")
    private String cacerts2Path;

    @Value("${certificats.key}")
    private String keyPath;

    private final HandleClient handleClient;

    private int responseMsg, index;
    private String pathKey, pass, adminHandle, prefix, privatePrefix, serverHandle, message;
    private AdminRecord admin;
    private PublicKeyAuthenticationInfo auth;
    private HandleResolver resolver;


    public void applyNodePreference(Preferences nodePreference){
        serverHandle = nodePreference.getUrlApiHandle();
        pass = nodePreference.getPassHandle();
        prefix = nodePreference.getPrefixIdHandle(); // exp : 20.500.11859
        privatePrefix = nodePreference.getPrivatePrefixHandle();
        
        pathKey = admprivPath;//("certificats/admpriv.bin");
        try {
            index = nodePreference.getIndexHandle(); // exp : 300
        } catch (Exception e) {
        }
        adminHandle = nodePreference.getAdminHandle(); // exp : 0.NA/20.500.11859
    }    
    
    public boolean connectHandle(){
        byte[] key = null;
        try {
            File f= new File(admprivPath);
            FileInputStream fs = new FileInputStream(f);
            key = Util.getBytesFromInputStream(fs);
            if (key == null || key.length == 0) {
                throw new RuntimeException("Failed to load private key. Key is null or empty.");
            }
        } catch (Throwable t) {
            message = "Cannot read private key " + admprivPath + ": " + t;
            log.debug("Cannot read private key " + admprivPath + ": " + t);
            return false;
        }

        // A HandleResolver object is used not just for resolution, but for
        // all handle operations(including create)
        resolver = new HandleResolver();

        // Check to see if the private key is encrypted.  If so, read in the
        // user's passphrase and decrypt.  Finally, convert the byte[] 
        // representation of the private key into a PrivateKey object.
        PrivateKey privkey = null;
        byte secKey[] = null;
        try {
            if (Util.requiresSecretKey(key)) {
                secKey = pass.getBytes();
            }
            key = Util.decrypt(key, secKey);
            privkey = Util.getPrivateKeyFromBytes(key, 0);
        } catch (Throwable t) {
            log.debug("Can't load private key in " + admprivPath + ": " + t);
            return false;
        }

        try {
            // Create a PublicKeyAuthenticationInfo object to pass to HandleResolver.
            // This is constructed with the admin handle, index, and PrivateKey as
            // arguments.
            auth = new PublicKeyAuthenticationInfo(adminHandle.getBytes("UTF8"),
                            index,
                            privkey);

            // We don't want to create a handle without an admin value-- otherwise
            // we would be locked out.  Give ourselves all permissions, even
            // ones that only apply for NA handles.
            admin = new AdminRecord(adminHandle.getBytes("UTF8"), index,
                    true, true, true, true, true, true,
                    true, true, true, true, true, true);

        } catch (Throwable t) {
            log.debug("\nError: " + t);
        }
        return true;
    }

    /**
     * peut créer un handle
     * 
     * @param handle
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     * @throws HandleException 
     */
    public boolean createHandle(String handle, String url) throws UnsupportedEncodingException, HandleException {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            //prefix by http by default
            url = "https://" + url;
        }
        HandleValue[] val = {new HandleValue(100, "URL", url)};

        // Now we can build our CreateHandleRequest object.  As its first
        // parameter it takes the handle we are going to create.  The second
        // argument is the array of initial values the handle should have.
        // The final argument is the authentication object that should be
        // used to gain permission to perform the creation.
        String handleId = prefix + "/" + handle;
        CreateHandleRequest req = new CreateHandleRequest(handleId.getBytes("UTF8"), val, auth);

        AbstractResponse response = resolver.processRequest(req);

        // The responseCode value for a response indicates the status of
        // the request.  A successful resolution will always return
        // RC_SUCCESS.  Failed resolutions could return one of several
        // response codes, including RC_ERROR, RC_INVALID_ADMIN, and
        // RC_INSUFFICIENT_PERMISSIONS.
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {
            //System.out.println("\nGot Response: \n" + response);
            message = response.toString();
            this.setResponseMsg(response.responseCode);
            return true;
        } else {
            //System.out.println("\nGot Error: \n" + response);
            message = response.toString();
            this.setResponseMsg(response.responseCode);
        }
        return false;
    }

    /**
     * Permet d'ajouter un identifiant Handle
     */
    public String addIdHandle(String privateUri, Preferences nodePreference) {

        if (nodePreference == null || !nodePreference.isUseHandle()) {
            return null;
        }

        var newId = nodePreference.getPrefixIdHandle() + "/" + nodePreference.getPrivatePrefixHandle() + getNewHandleId(nodePreference);

        log.debug("avant l'appel à HandleClient");
        var jsonData = handleClient.getJsonData(nodePreference.getCheminSite() + privateUri);//"?idc=" + idConcept + "&idt=" + idThesaurus);
        log.debug("avant le put ");
        var idHandle = handleClient.putHandle(nodePreference.getPassHandle(), nodePreference.getPathKeyHandle(),
                nodePreference.getPathCertHandle(), nodePreference.getUrlApiHandle(), newId, jsonData);
        if (idHandle == null) {
            message = handleClient.getMessage();
            return null;
        }
        return idHandle;
    }

    /**
     * permet de genérer un identifiant unique pour Handle on controle la
     * présence de l'identifiant sur handle.net si oui, on regénère un autre.
     */
    private String getNewHandleId(Preferences nodePreference) {

        boolean duplicateId = true;
        String idHandle = null;

        while (duplicateId) {
            idHandle = ToolsHelper.getNewId(10, false, false);
            if (!handleClient.isHandleExist(nodePreference.getUrlApiHandle(),
                    nodePreference.getPrefixIdHandle() + "/" + idHandle)) {
                duplicateId = false;
            }
            if (!handleClient.isHandleExist(
                    " https://hdl.handle.net/",
                    nodePreference.getPrefixIdHandle() + "/" + idHandle)) {
                duplicateId = false;
            }
        }
        return idHandle;
    }

    /**
     * Permet de supprimer un identifiant Handle de la
     * plateforme (handle.net) via l'API REST
     */
    public boolean deleteIdHandle(String idHandle, Preferences nodePreference) {

        if (nodePreference == null || !nodePreference.isUseHandle()) {
            return false;
        }

        var status = handleClient.deleteHandle(nodePreference.getPassHandle(), nodePreference.getPathKeyHandle(),
                nodePreference.getPathCertHandle(), nodePreference.getUrlApiHandle(), idHandle);
        if (!status) {
            message = handleClient.getMessage();
            return false;
        }

        return true;
    }

    /**
     * Permet de supprimer tous les identifiants Handle
     * de la plateforme (handle.net) via l'API REST pour un thésaurus donné
     * suite à une suppression d'un thésaurus
     */
    public boolean deleteAllIdHandle(List<String> tabIdHandle, Preferences nodePreference) {

        if (nodePreference == null || !nodePreference.isUseHandle()) {
            return false;
        }

        boolean first = true;
        for (String idHandle : tabIdHandle) {
            var status = handleClient.deleteHandle(nodePreference.getPassHandle(), nodePreference.getPathKeyHandle(),
                    nodePreference.getPathCertHandle(), nodePreference.getUrlApiHandle(), idHandle);
            if (!status) {
                if (first) {
                    message = "Id handle non supprimé :\n ";
                    first = false;
                }
                message = message + idHandle + " ## ";
            }
        }
        return true;
    }

    /**
     * peut supprimer un Handle
     */
    public boolean deleteHandle(String handleId) throws HandleException {
        DeleteHandleRequest req = new DeleteHandleRequest(Util.encodeString(handleId), auth);
        AbstractResponse response = resolver.processRequest(req);
        if (response == null || response.responseCode != AbstractMessage.RC_SUCCESS) {
            System.out.println("error deleting '" + handleId + "': " + response);
            this.setResponseMsg(response.responseCode);
        } else {
            System.out.println("deleted " + handleId);
            return true;
        }
        return false;
    }
}
