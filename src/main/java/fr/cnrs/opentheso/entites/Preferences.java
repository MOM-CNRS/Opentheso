package fr.cnrs.opentheso.entites;

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


@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "preferences")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPref;
    private String idThesaurus;
    private String sourceLang;
    private Integer identifierType;
    private String cheminSite;
    private String idNaan;

    // Paramètres Handle
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

    // Paramètres Ark
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

    // Pour la génération des identifants Ark en local
    private boolean useArkLocal;
    private String naanArkLocal;
    private String prefixArkLocal;

    @Column(name = "sizeidArkLocal")
    private Integer sizeIdArkLocal;

    private boolean breadcrumb;

    // Activer l'onglet ConceptTree(le dernnier)
    @Column(name = "useconcepttree")
    private boolean useConceptTree;

    // Afficher le nom des personnes qui ont modifié les concepts
    private boolean displayUserName;

    private boolean suggestion;
    private boolean useCustomRelation;
    private boolean uppercaseForArk;

    // Masquer les notes à l'interface publique
    @Column(name = "showHistorynote")
    private boolean showHistoryNote;
    @Column(name = "showEditorialnote")
    private boolean showEditorialNote;

    private boolean useHandleWithCertificat;
    private String adminHandle;
    private Integer indexHandle;

    // Gérer le module de traduction via Deepl
    private boolean useDeeplTranslation;
    private String deeplApiKey;

    private boolean webservices;

    // Permet d'afficher un lien vers la ressource dans Koha Pro
    @Column(name = "koha_link")
    private boolean kohaLink;

    // Pour la gestion des identifiants Ark via le serveur OpenArk
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
