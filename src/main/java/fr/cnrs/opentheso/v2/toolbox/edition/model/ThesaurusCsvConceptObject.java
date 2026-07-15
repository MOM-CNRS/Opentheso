package fr.cnrs.opentheso.v2.toolbox.edition.model;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import lombok.Data;

import java.util.ArrayList;

@Data
public class ThesaurusCsvConceptObject {

    private String idConcept;
    private String uri;
    private String localId;
    private String arkId;
    private String idTerm;
    private String type;
    private String conceptType;
    private boolean deprecated;
    private String notation;

    private ArrayList<ThesaurusCsvConceptLabel> prefLabels;
    private ArrayList<ThesaurusCsvConceptLabel> altLabels;
    private ArrayList<ThesaurusCsvConceptLabel> hiddenLabels;

    private ArrayList<ThesaurusCsvConceptLabel> note;
    private ArrayList<ThesaurusCsvConceptLabel> definitions;
    private ArrayList<ThesaurusCsvConceptLabel> scopeNotes;
    private ArrayList<ThesaurusCsvConceptLabel> examples;
    private ArrayList<ThesaurusCsvConceptLabel> historyNotes;
    private ArrayList<ThesaurusCsvConceptLabel> changeNotes;
    private ArrayList<ThesaurusCsvConceptLabel> editorialNotes;

    private ArrayList<String> broaders;
    private ArrayList<String> narrowers;
    private ArrayList<String> relateds;
    private ArrayList<NodeIdValue> customRelations;

    private ArrayList<String> exactMatchs;
    private ArrayList<String> closeMatchs;
    private ArrayList<String> broadMatchs;
    private ArrayList<String> narrowMatchs;
    private ArrayList<String> relatedMatchs;

    private String latitude;
    private String longitude;
    private String gps;

    private ArrayList<String> members;
    private String superOrdinate;
    private ArrayList<String> subGroups;
    private ArrayList<String> replacedBy;

    private ArrayList<NodeImage> images;
    private ArrayList<String> externalResources;
    private ArrayList<String> memberOfFacets;

    private String created;
    private String modified;
    private ArrayList<NodeIdValue> alignments;

    public ThesaurusCsvConceptObject() {
        prefLabels = new ArrayList<>();
        altLabels = new ArrayList<>();
        hiddenLabels = new ArrayList<>();

        note = new ArrayList<>();
        definitions = new ArrayList<>();
        scopeNotes = new ArrayList<>();
        examples = new ArrayList<>();
        historyNotes = new ArrayList<>();
        changeNotes = new ArrayList<>();
        editorialNotes = new ArrayList<>();

        broaders = new ArrayList<>();
        narrowers = new ArrayList<>();
        relateds = new ArrayList<>();
        customRelations = new ArrayList<>();

        exactMatchs = new ArrayList<>();
        closeMatchs = new ArrayList<>();
        broadMatchs = new ArrayList<>();
        narrowMatchs = new ArrayList<>();
        relatedMatchs = new ArrayList<>();

        members = new ArrayList<>();
        alignments = new ArrayList<>();

        subGroups = new ArrayList<>();
        replacedBy = new ArrayList<>();
        images = new ArrayList<>();
        externalResources = new ArrayList<>();
        memberOfFacets = new ArrayList<>();
        conceptType = null;
    }

    public void clear() {
        if (prefLabels != null) {
            prefLabels.clear();
        }
        if (altLabels != null) {
            altLabels.clear();
        }
        if (hiddenLabels != null) {
            hiddenLabels.clear();
        }
        if (note != null) {
            note.clear();
        }
        if (definitions != null) {
            definitions.clear();
        }
        if (scopeNotes != null) {
            scopeNotes.clear();
        }
        if (examples != null) {
            examples.clear();
        }
        if (historyNotes != null) {
            historyNotes.clear();
        }
        if (changeNotes != null) {
            changeNotes.clear();
        }
        if (editorialNotes != null) {
            editorialNotes.clear();
        }
        if (broaders != null) {
            broaders.clear();
        }
        if (narrowers != null) {
            narrowers.clear();
        }
        if (relateds != null) {
            relateds.clear();
        }
        if (customRelations != null) {
            customRelations.clear();
        }
        if (exactMatchs != null) {
            exactMatchs.clear();
        }
        if (closeMatchs != null) {
            closeMatchs.clear();
        }
        if (broadMatchs != null) {
            broadMatchs.clear();
        }
        if (narrowMatchs != null) {
            narrowMatchs.clear();
        }
        if (relatedMatchs != null) {
            relatedMatchs.clear();
        }
        if (members != null) {
            members.clear();
        }
        if (alignments != null) {
            alignments.clear();
        }
        if (memberOfFacets != null) {
            memberOfFacets.clear();
        }

        conceptType = null;
        gps = null;
    }
}
