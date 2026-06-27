package fr.cnrs.opentheso.v2.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "V2Preferences")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "preferences")
public class PreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPref;

    private String idThesaurus;
    private String sourceLang;
    private Integer identifierType;
    private String cheminSite;
    private String idNaan;

    private boolean useHandle;
    private String userHandle;
    private String passHandle;
    private String pathKeyHandle;
    private String pathCertHandle;
    private String urlApiHandle;

    @Column(name = "prefixHandle")
    private String prefixIdHandle;
    private String privatePrefixHandle;

    @Column(name = "preferredname")
    private String preferredName;

    private String originalUri;
    private boolean originalUriIsArk;
    private boolean originalUriIsHandle;

    private String uriArk;
    private boolean useArk;
    private String serverArk;
    private String prefixArk;
    private String userArk;
    private String passArk;

    private boolean generateHandle;
    private boolean autoExpandTree;
    private boolean sortByNotation;
    private boolean treeCache;
    private boolean originalUriIsDoi;

    private boolean useArkLocal;
    private String naanArkLocal;
    private String prefixArkLocal;

    @Column(name = "sizeidArkLocal")
    private Integer sizeIdArkLocal;

    private boolean breadcrumb;

    @Column(name = "useconcepttree")
    private boolean useConceptTree;

    private boolean displayUserName;
    private boolean suggestion;
    private boolean useCustomRelation;
    private boolean uppercaseForArk;

    @Column(name = "showHistorynote")
    private boolean showHistoryNote;

    @Column(name = "showEditorialnote")
    private boolean showEditorialNote;

    private boolean useHandleWithCertificat;
    private String adminHandle;
    private Integer indexHandle;

    private boolean useDeeplTranslation;
    private String deeplApiKey;

    private boolean webservices;

    @Column(name = "koha_link")
    private boolean kohaLink;

    @Column(name = "use_openark")
    private boolean useOpenArk;

    @Column(name = "server_openark")
    private String serverOpenArk;

    @Column(name = "naan_openark")
    private String naanOpenArk;

    @Column(name = "prefix_openark")
    private String prefixOpenArk;

    @Column(name = "api_key_openark")
    private String apiKeyOpenArk;
}
