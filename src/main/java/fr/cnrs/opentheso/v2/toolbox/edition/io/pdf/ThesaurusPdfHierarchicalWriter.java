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
        this.exportThesaurusId = xmlDocument.getConceptScheme() != null
                && xmlDocument.getConceptScheme().getThesaurus() != null
                ? xmlDocument.getConceptScheme().getThesaurus().getId_thesaurus()
                : null;
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

        HierarchicalWriteContext context = new HierarchicalWriteContext(
                new HierarchicalTreeState(labels, idToChildId, resourceChecked, writePdfSettings),
                new HierarchicalDetails(matchs, images, gps, notesDiff));
        traitement(paragraphs, codeLanguage1, codeLanguage2, false, notes, concepts, context);

        if (StringUtils.isNotEmpty(codeLanguage2)) {
            traitement(paragraphTradList, codeLanguage2, codeLanguage1, true, notesTraduction, concepts, context);
        }
    }

    private void traitement(List<Paragraph> paragraphs, String codeLanguage1, String codeLanguage2, boolean isTrad,
                            HashMap<String, ArrayList<String>> idToDoc, List<SKOSResource> concepts,
                            HierarchicalWriteContext context) {

        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(concepts, sortForHiera(isTrad, codeLanguage1, codeLanguage2, context.tree().labels(),
                context.tree().idToChildId(), idToDoc, context.details().matchs(), context.details().gps(),
                context.details().images(), context.tree().resourceChecked(), context.details().notesDiff(), termLookup));

        for (SKOSResource concept : concepts) {
            writeRootConcept(concept, paragraphs, idToDoc, context);
        }
    }

    private void writeRootConcept(SKOSResource concept, List<Paragraph> paragraphs,
                                  HashMap<String, ArrayList<String>> idToDoc, HierarchicalWriteContext context) {
        String conceptID = concept.getIdentifier();
        if (!isAtRoot(conceptID, context.tree().idToChildId())) {
            return;
        }
        String name = context.tree().labels().get(conceptID);
        if (name == null) {
            name = "";
        }

        Paragraph paragraph = new Paragraph();
        Anchor anchor = new Anchor(name + " (" + conceptID + ")", context.tree().writePdfSettings().getTermFont());
        anchor.setReference(uriResolver.getUriForConcept(
                exportPreferences, exportThesaurusId, concept.getIdentifier(), concept.getArkId(), concept.getArkId()));
        paragraph.add(anchor);
        paragraphs.add(paragraph);

        String indentation = "";
        addConceptDetails(conceptID, indentation, paragraphs, idToDoc, context);
        addConcept(conceptID, indentation, paragraphs, idToDoc, context);
    }

    private boolean isAtRoot(String conceptID, HashMap<String, List<String>> idToChildId) {
        Iterator<String> i = idToChildId.keySet().iterator();
        while (i.hasNext()) {
            List<String> valeur = idToChildId.get(i.next());
            for (String id : valeur) {
                if (id.equals(conceptID)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void addConcept(String id, String indentation, List<Paragraph> paragraphs,
                            HashMap<String, ArrayList<String>> idToDoc, HierarchicalWriteContext context) {

        indentation += ".......";

        List<String> childList = context.tree().idToChildId().get(id);
        if (childList == null) {
            return;
        }
        String idArk;
        for (String idFils : childList) {
            String name = context.tree().labels().get(idFils);
            if (name == null) {
                name = "";
            }

            Paragraph paragraph = new Paragraph();
            Anchor anchor = new Anchor(indentation + name + " (" + idFils + ")", context.tree().writePdfSettings().getTextFont());
            idArk = uriResolver.getIdArk(exportPreferences, exportThesaurusId, idFils);
            anchor.setReference(uriResolver.getUriForConcept(exportPreferences, exportThesaurusId, idFils, idArk, idArk));
            paragraph.add(anchor);
            paragraphs.add(paragraph);

            addConceptDetails(idFils, indentation, paragraphs, idToDoc, context);
            addConcept(idFils, indentation, paragraphs, idToDoc, context);
        }
    }

    private void addConceptDetails(String key, String indentation, List<Paragraph> paragraphs,
                                   HashMap<String, ArrayList<String>> idToDoc, HierarchicalWriteContext context) {

        String space = getSpace(indentation);
        addNotes(paragraphs, space, idToDoc.get(key), context.details().notesDiff().get(key), context.tree().writePdfSettings());
        addMatchs(paragraphs, context.details().matchs().get(key), space, context.tree().writePdfSettings());
        addGpsCoordiantes(paragraphs, context.details().gps().get(key), space, context.tree().writePdfSettings());
        if (isToogleExportImage) {
            ThesaurusPdfImageEmbedder.addImages(
                    paragraphs,
                    context.details().images().get(key),
                    indentation.length() * 2.9f,
                    context.tree().writePdfSettings()
            );
        }
    }

    private record HierarchicalTreeState(
            HashMap<String, String> labels,
            HashMap<String, List<String>> idToChildId,
            ArrayList<String> resourceChecked,
            ThesaurusPdfSettings writePdfSettings
    ) {
    }

    private record HierarchicalDetails(
            HashMap<String, ArrayList<String>> matchs,
            HashMap<String, ArrayList<NodeImage>> images,
            HashMap<String, List<String>> gps,
            HashMap<String, ArrayList<Integer>> notesDiff
    ) {
    }

    private record HierarchicalWriteContext(HierarchicalTreeState tree, HierarchicalDetails details) {
    }

    private String getSpace(String indentation) {
        return " ".repeat(indentation.length());
    }

    private void addNotes(List<Paragraph> paragraphs, String space, ArrayList<String> idToDoc, ArrayList<Integer> idTradDiff, ThesaurusPdfSettings writePdfSettings) {

        int docCount = 0;
        if (CollectionUtils.isNotEmpty(idTradDiff)) {
            docCount = (int) idTradDiff.stream().filter(traduction -> traduction == SKOSProperty.NOTE).count();
        }

        AtomicInteger docWrite = new AtomicInteger();
        if (CollectionUtils.isNotEmpty(idToDoc)) {
            idToDoc.forEach(document  -> {
                paragraphs.add(new Paragraph(space + document, writePdfSettings.getHieraInfoFont()));
                docWrite.getAndIncrement();
            });
        }

        if (docWrite.get() < docCount) {
            for (int i = 0; i < docCount; i++) {
                paragraphs.add(new Paragraph(space + "-", writePdfSettings.getHieraInfoFont()));
            }
        }
    }

    private void addMatchs(List<Paragraph> paragraphs, ArrayList<String> matchs, String space, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(matchs)) {
            matchs.forEach(match -> paragraphs.add(new Paragraph(space + match, writePdfSettings.getHieraInfoFont())));
        }
    }

    private void addGpsCoordiantes(List<Paragraph> paragraphs, List<String> gps, String space, ThesaurusPdfSettings writePdfSettings) {
        if (CollectionUtils.isNotEmpty(gps)) {
            paragraphs.add(new Paragraph(space + "GPS : (" + gps.stream().collect(Collectors.joining(", ")) + ")", writePdfSettings.getHieraInfoFont()));
        }
    }
}
