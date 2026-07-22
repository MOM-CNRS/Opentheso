package fr.cnrs.opentheso.bean.setting;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.services.HomePageService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.security.CryptoService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.io.Serializable;
import java.util.List;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "preferenceBean")
public class PreferenceBean implements Serializable {

    private final SelectedTheso selectedTheso;
    private final PreferenceService preferenceService;
    private final HomePageService homePageService;
    private final ThesaurusService thesaurusService;
    private final RoleOnThesaurusBean roleOnThesaurus;
    private final CryptoService cryptoService;

    private String uriType;
    private Preferences preferences;
    private List<NodeLangTheso> languagesOfThesaurus;

    private String newPassArk;
    private String actualPassArk;

    private String newApiKeyOpenArk;
    private String actualApiKeyOpenArk;

    private String newPassHandle;
    private String actualPassHandle;

    private String newDeeplApiKey;
    private String actualDeeplApiKey;



/*
    @Inject
    public PreferenceBean(
            SelectedTheso selectedTheso,
            PreferenceService preferenceService,
            HomePageService homePageService,
            ThesaurusService thesaurusService,
            RoleOnThesaurusBean roleOnThesaurus,
            CryptoService cryptoService
    ) {
        this.selectedTheso = selectedTheso;
        this.preferenceService = preferenceService;
        this.homePageService = homePageService;
        this.thesaurusService = thesaurusService;
        this.roleOnThesaurus = roleOnThesaurus;
        this.cryptoService = cryptoService;
    }*/

    public void init() {

        if (selectedTheso.getCurrentIdTheso() == null) {
            return;
        }

        preferences = preferenceService.getThesaurusPreferences(selectedTheso.getCurrentIdTheso());
        if (this.preferences == null) {
            log.error("Aucun paramètre n'est trouvé pour le thésaurus id {}", selectedTheso.getCurrentIdTheso());
            return;
        }
        actualPassArk = preferences.getPassArk();
        preferences.setPassArk("");

        ///  gestion de la Clé d'API avec cryptage
        actualApiKeyOpenArk = preferences.getApiKeyOpenArk();
     //   SimpleCrypto crypto = new SimpleCrypto(actualApiKeyOpenArk);
        preferences.setApiKeyOpenArk("");

        actualPassHandle = preferences.getPassHandle();
        preferences.setPassHandle("");

        actualDeeplApiKey = preferences.getDeeplApiKey();
        preferences.setDeeplApiKey("");

        languagesOfThesaurus = thesaurusService.getAllUsedLanguagesOfThesaurusNode(selectedTheso.getCurrentIdTheso(),
                preferences.getSourceLang());

        uriType = "uri";
        if (this.preferences.isOriginalUriIsHandle()) {
            uriType = "handle";
        } else if (this.preferences.isOriginalUriIsArk()) {
            uriType = "ark";
        } else if (this.preferences.isOriginalUriIsDoi()) {
            uriType = "doi";
        }
    }

    public void updateSelectedServer(String selectedServer){

        switch (selectedServer) {
            case "ark":
                preferences.setUseArk(preferences.isUseArk());
                preferences.setUseArkLocal(false);
                preferences.setUseHandle(false);
                preferences.setUseOpenArk(false);
                break;
            case "arklocal":
                preferences.setUseArk(false);
                preferences.setUseArkLocal(preferences.isUseArkLocal());
                preferences.setUseHandle(false);
                preferences.setUseOpenArk(false);
                break;
            case "handle":
                preferences.setUseArk(false);
                preferences.setUseArkLocal(false);
                preferences.setUseHandle(preferences.isUseHandle());
                preferences.setUseOpenArk(false);
                break;
            case "openark":
                preferences.setUseArk(false);
                preferences.setUseArkLocal(false);
                preferences.setUseHandle(false);
                preferences.setUseOpenArk(preferences.isUseOpenArk());
                break;
        }
        preferenceService.setIdentifierFlags(selectedTheso.getCurrentIdTheso(), preferences.isUseArk(), preferences.isUseArkLocal(), preferences.isUseHandle(), preferences.isUseOpenArk());
    }
    
    public String getGoogleAnalytics() {
        return homePageService.getCodeGoogleAnalytics();
    }

    public void savePreference() {

        if (uriType == null) {
            return;
        }

        if(preferenceService.isPreferredNameExist(preferences.getPreferredName())){
            MessageUtils.showErrorMessage("PreferredName existe déjà, veuillez en choisir un autre ! ");
            return;
        }

        // contrôle du mot de passe Ark
        if(StringUtils.isNotBlank(newPassArk)){
            preferences.setPassArk(newPassArk);
        } else {
            preferences.setPassArk(actualPassArk);
        }

        // contrôle de la clé d'API de OpenArk
        String valueToStore;
        if (newApiKeyOpenArk != null && !newApiKeyOpenArk.isBlank()) {
            valueToStore = cryptoService.encrypt(newApiKeyOpenArk);
        } else {
            valueToStore = actualApiKeyOpenArk; // déjà chiffrée
        }

        preferences.setApiKeyOpenArk(valueToStore);
        preferenceService.updateAllPreferenceUser(preferences);

        // contrôle du mot de passe Handle
        if(StringUtils.isNotBlank(newPassHandle)){
            preferences.setPassHandle(newPassHandle);
        } else {
            preferences.setPassHandle(actualPassHandle);
        }

        // contrôle de la clé d'API de Deepl
        if(StringUtils.isNotBlank(newDeeplApiKey)){
            preferences.setDeeplApiKey(newDeeplApiKey);
        } else {
            preferences.setDeeplApiKey(actualDeeplApiKey);
        }

        preferences.setOriginalUriIsArk(uriType.equalsIgnoreCase("ark"));
        preferences.setOriginalUriIsHandle(uriType.equalsIgnoreCase("handle"));
        preferenceService.updateAllPreferenceUser(preferences);

        roleOnThesaurus.setNodePreference(preferenceService.getThesaurusPreferences(selectedTheso.getCurrentIdTheso()));

        MessageUtils.showInformationMessage("Préférences enregistrées avec succès");
        newPassArk = null;
        newApiKeyOpenArk = null;
        newPassHandle = null;
        newDeeplApiKey = null;
        init();
    }
}
