package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.Paragraph;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.terms.ConceptPreferredTermLookup;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionPdfUriResolver;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.cnrs.opentheso.models.skosapi.SKOSResource.sortForHiera;


@Component
@RequiredArgsConstructor
public class ThesaurusPdfHierarchicalWriter {

    private final ThesaurusEditionPdfUriResolver uriResolver;
    private final ConceptPreferredTermLookup termLookup;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    private boolean isToogleExportImage;
    private Preferences exportPreferences;
    private String exportThesaurusId;

    public void writeHierachiquePDF(List<Paragraph> paragraphs, List<Paragraph> paragraphTradList, String codeLanguage1,
                                    String codeLanguage2, ThesaurusPdfSettings writePdfSettings, SKOSXmlDocument xmlDocument, boolean isToogleExportImage) {
        this.isToogleExportImage = isToogleExportImage;
        this.exportThesaurusId = xmlDocument.getConceptScheme().getThesaurus().getId_thesaurus();
        this.exportPreferences = toolboxPreferencePersistence.findPreferences(exportThesaurusId);
        HashMap<String, String> labels = new HashMap<>();
        HashMap<String, List<String>> idToChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> notes = new HashMap<>();
        HashMap<String, ArrayList<String>> notesTraduction = new HashMap<>();
        HashMap<String, ArrayList<String>> matchs = new HashMap<>();
        HashMap<String, ArrayList<NodeImage>> images = new HashMap<>();
        HashMap<String, List<String>> gps = new HashMap<>();
        HashMap<String, ArrayList<Integer>> notesDiff = new HashMap<>();
        ArrayList<String> resourceChecked = new ArrayList<>();

        List<SKOSResource> concepts = xmlDocument.getConceptList();

        traitement(paragraphs, codeLanguage1, codeLanguage2, false, notes, concepts, labels, idToChildId,
                writePdfSettings, gps, matchs, images, notesDiff, resourceChecked);

        if (StringUtils.isNotEmpty(codeLanguage2)) {
            traitement(paragraphTradList, codeLanguage2, codeLanguage1, true, notesTraduction, concepts, labels,
                    idToChildId, writePdfSettings, gps, matchs, images, notesDiff, resourceChecked);
        }
    }

    private void traitement(List<Paragraph> paragraphs, String codeLanguage1, String codeLanguage2, boolean isTrad,
                            HashMap<String, ArrayList<String>> idToDoc, List<SKOSResource> concepts,
                            HashMap<String, String> labels, HashMap<String, List<String>> idToChildId,
                            ThesaurusPdfSettings writePdfSettings, HashMap<String, List<String>> gps,
                            HashMap<String, ArrayList<String>> matchs, HashMap<String, ArrayList<NodeImage>> images,
                            HashMap<String, ArrayList<Integer>> notesDiff, ArrayList<String> resourceChecked) {

        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(concepts, sortForHiera(isTrad, codeLanguage1, codeLanguage2, labels,
                idToChildId, idToDoc, matchs, gps, images, resourceChecked, notesDiff, termLookup));

        for (SKOSResource concept : concepts) {

            boolean isAtRoot = true;
            String conceptID = concept.getIdentifier();
            Iterator i = idToChildId.keySet().iterator();
            while (i.hasNext()) {
                ArrayList<String> valeur = (ArrayList<String>) idToChildId.get((String) i.next());
                for (String id : valeur) {
                    if (id.equals(conceptID)) {
                        isAtRoot = false;
                    }
                }
            }

            if (isAtRoot) {
                String name = labels.get(conceptID);
                if (name == null) {
                    name = "";
                }

                Paragraph paragraph = new Paragraph();
                Anchor anchor = new Anchor(name + " (" + conceptID + ")", writePdfSettings.termFont);
                anchor.setReference(uriResolver.getUriForConcept(
                        exportPreferences, exportThesaurusId, concept.getIdentifier(), concept.getArkId(), concept.getArkId()));
                paragraph.add(anchor);
                paragraphs.add(paragraph);

                String indentation = "";
                addConceptDetails(conceptID, indentation, paragraphs, idToDoc, writePdfSettings, gps, images, matchs, notesDiff);
                addConcept(conceptID, indentation, paragraphs, idToDoc, labels, idToChildId, writePdfSettings, gps, images, matchs, notesDiff);
            }
        }
    }


    private void addConcept(String id, String indentation, List<Paragraph> paragraphs, HashMap<String, ArrayList<String>> idToDoc,
                            HashMap<String, String> labels, HashMap<String, List<String>> idToChildId,
                            ThesaurusPdfSettings writePdfSettings, HashMap<String, List<String>> gps,
                            HashMap<String, ArrayList<NodeImage>> images, HashMap<String, ArrayList<String>> matchs,
                            HashMap<String, ArrayList<Integer>> notesDiff) {

        indentation += ".......";

        List<String> childList = idToChildId.get(id);
        if (childList == null) {
            return;
        }
        String idArk;
        for (String idFils : childList) {
            String name = labels.get(idFils);
            if (name == null) {
                name = "";
            }

            Paragraph paragraph = new Paragraph();
            Anchor anchor = new Anchor(indentation + name + " (" + idFils + ")", writePdfSettings.textFont);
            idArk = uriResolver.getIdArk(exportPreferences, exportThesaurusId, idFils);
            anchor.setReference(uriResolver.getUriForConcept(exportPreferences, exportThesaurusId, idFils, idArk, idArk));
            paragraph.add(anchor);
            paragraphs.add(paragraph);

            addConceptDetails(idFils, indentation, paragraphs, idToDoc, writePdfSettings, gps, images, matchs, notesDiff);
            addConcept(idFils, indentation, paragraphs, idToDoc, labels, idToChildId, writePdfSettings, gps, images, matchs, notesDiff);
        }
    }

    private void addConceptDetails(String key, String indentation, List<Paragraph> paragraphs, HashMap<String,
            ArrayList<String>> idToDoc, ThesaurusPdfSettings writePdfSettings, HashMap<String, List<String>> gps,
            HashMap<String, ArrayList<NodeImage>> images, HashMap<String, ArrayList<String>> matchs,
            HashMap<String, ArrayList<Integer>> notesDiff) {

        String space = getSpace(indentation);
        addNotes(paragraphs, space, idToDoc.get(key), notesDiff.get(key), writePdfSettings);
        addMatchs(paragraphs, matchs.get(key), space, writePdfSettings);
        addGpsCoordiantes(paragraphs, gps.get(key), space, writePdfSettings);
        if (isToogleExportImage) {
            ThesaurusPdfImageEmbedder.addImages(
                    paragraphs,
                    images.get(key),
                    indentation.length() * 2.9f,
                    writePdfSettings
            );
        }
    }

    private String getSpace(String indentation) {
        String space = "";
        for (int i = 0; i < indentation.length(); i++) {
            space += " ";
        }
        return space;
    }

    private void addNotes(List<Paragraph> paragraphs, String space, ArrayList<String> idToDoc, ArrayList<Integer> idTradDiff, ThesaurusPdfSettings writePdfSettings) {

        int docCount = 0;
        if (CollectionUtils.isNotEmpty(idTradDiff)) {
            docCount = (int) idTradDiff.stream().filter(traduction -> traduction == SKOSProperty.NOTE).count();
        }

        AtomicInteger docWrite = new AtomicInteger();
        if (CollectionUtils.isNotEmpty(idToDoc)) {
            idToDoc.stream().forEach(document  -> {
                paragraphs.add(new Paragraph(space + document, writePdfSettings.hieraInfoFont));
                docWrite.getAndIncrement();
            });
        }

        if (docWrite.get() < docCount) {
            for (int i = 0; i < docCount; i++) {
                paragraphs.add(new Paragraph(space + "-", writePdfSettings.hieraInfoFont));
            }
        }
    }

    private void addMatchs(List<Paragraph> paragraphs, ArrayList<String> matchs, String space, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(matchs)) {
            matchs.stream().forEach(match -> paragraphs.add(new Paragraph(space + match, writePdfSettings.hieraInfoFont)));
        }
    }

    private void addGpsCoordiantes(List<Paragraph> paragraphs, List<String> gps, String space, ThesaurusPdfSettings writePdfSettings) {
        if (CollectionUtils.isNotEmpty(gps)) {
            paragraphs.add(new Paragraph(space + "GPS : (" + gps.stream().collect(Collectors.joining(", ")) + ")", writePdfSettings.hieraInfoFont));
        }
    }
}
