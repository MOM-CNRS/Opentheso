package fr.cnrs.opentheso.models.candidats;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
public class CandidatDto implements Serializable {

    private String idTerm;
    private String idConcepte;
    private String idThesaurus;
    private String lang;
    private String adminMessage;
    
    private String createdBy;
    private int createdById;

    private String createdByAdmin;
    private int createdByIdAdmin;
    
    private String user;
    private int userId;
    
    private String domaine;
    private String nomPref;
    private Date creationDate;
    private Date insertionDate;
    
    private String statut;
    private int nbrDemande;
    private int nbrParticipant;
    private boolean voted;
    private boolean noteVoted;
    private boolean downVoted;
    private int nbrVote;
    private int nbrNoteVote;
    private int nbrDownVote;

    private String noteApplication;
    private List<String> defenitions;
    private List<String> participants;
    private List<TraductionDto> traductions;
    private List<CorpusDto> corpus;
    private List<MessageDto> messages;

    private List<NodeAlignment> alignments;
    
    // ajoutés par Miled 
    private List<NodeIdValue> collections;
    private List<NodeNote> nodeNotes;    

    private List<NodeIdValue> termesGenerique;
    private List<NodeIdValue> termesAssocies;
    
    private List<String> employePourList;    
    private String employePour;
    private List<NodeImage> images;

    
    public CandidatDto() {
        corpus = new ArrayList<>();
        messages = new ArrayList<>();
        traductions = new ArrayList<>();
        defenitions = new ArrayList<>();
        termesGenerique = new ArrayList<>();
        termesAssocies = new ArrayList<>();
        employePour = "";
        participants = new ArrayList<>();
        collections = new ArrayList<>();
        adminMessage = "";
        employePourList = new ArrayList<>();
        alignments = new ArrayList<>();
    }

    public CandidatDto(String nomPref) {
        idConcepte = null;
        this.nomPref = nomPref;
    }

    public CandidatDto(CandidatDto source) {
        this.idTerm = source.getIdTerm();
        this.idConcepte = source.getIdConcepte();
        this.idThesaurus = source.getIdThesaurus();
        this.userId = source.getUserId();
        this.user = source.getUser();
        this.nomPref = source.getNomPref();
        this.collections = source.getCollections();
        this.termesGenerique = source.getTermesGenerique();
        this.defenitions = source.getDefenitions();
        this.noteApplication = source.getNoteApplication();
        this.termesAssocies = source.getTermesAssocies();
        this.corpus = source.getCorpus();
        this.messages = source.getMessages();
        this.traductions = source.getTraductions();
        this.adminMessage = source.getAdminMessage();
        this.domaine = source.getDomaine();
        this.alignments = source.getAlignments();
    }
    
}
