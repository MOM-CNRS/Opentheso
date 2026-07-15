package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSMatch;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionPdfUriResolver;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static fr.cnrs.opentheso.models.skosapi.SKOSResource.sortAlphabeticInLang;


@Component
@RequiredArgsConstructor
public class ThesaurusPdfAlphabeticWriter {

    private final ThesaurusEditionPdfUriResolver uriResolver;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    private final static String TAB_NIVEAU = "    ";
    private final static String ID = TAB_NIVEAU + "ID: ";
    private final static String USE = TAB_NIVEAU + "USE: ";
    private final static String GPS = TAB_NIVEAU + "GPS: ";

    private boolean isToogleExportImage;
    private Preferences exportPreferences;
    private String exportThesaurusId;

    public void writeAlphabetiquePDF(SKOSXmlDocument xmlDocument, List<Paragraph> paragraphs, List<Paragraph> paragraphTradList,
                                     String codeLanguage1, String codeLanguage2, ThesaurusPdfSettings writePdfSettings, boolean isToogleExportImage) {
        this.isToogleExportImage = isToogleExportImage;
        this.exportThesaurusId = xmlDocument.getConceptScheme().getThesaurus().getId_thesaurus();
        this.exportPreferences = toolboxPreferencePersistence.findPreferences(exportThesaurusId);
        var concepts = xmlDocument.getConceptList();
        List<String> resourceChecked = new ArrayList<>();
        HashMap<String, List<Integer>> traductions = new HashMap<>();
        HashMap<String, String> labels = new HashMap<>();

        traitement(paragraphs, false, codeLanguage1, codeLanguage2, writePdfSettings, concepts, resourceChecked, traductions, labels);

        if (StringUtils.isNotEmpty(codeLanguage2)) {
            traitement(paragraphTradList, true, codeLanguage2, codeLanguage1, writePdfSettings, concepts, resourceChecked, traductions, labels);
        }
    }

    private void traitement(List<Paragraph> paragraphs, boolean isTrad, String codeLanguage1, String codeLanguage2,
                            ThesaurusPdfSettings writePdfSettings, ArrayList<SKOSResource> concepts, List<String> resourceChecked,
                            HashMap<String, List<Integer>> traductions, HashMap<String, String> labels) {

        // Trier les concepts selon leurs labels
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(concepts, sortAlphabeticInLang(isTrad, codeLanguage1, codeLanguage2, labels, traductions, resourceChecked));

        // Construire la liste des concepts sous forme d'une suite des paragraphs
        for (SKOSResource concept : concepts) {
            writeTerm(concept, paragraphs, codeLanguage1, codeLanguage2, writePdfSettings, traductions, labels);
        }
    }

    private void writeTerm(SKOSResource concept, List<Paragraph> paragraphs, String codeLanguage1, String codeLanguage2,
                           ThesaurusPdfSettings writePdfSettings, HashMap<String, List<Integer>> traductions, HashMap<String, String> labels) {

        String idFromUri = concept.getIdentifier();
        if(addLabels(paragraphs, concept.getLabelsList(), codeLanguage1, codeLanguage2, idFromUri, concept.getArkId(), writePdfSettings, traductions)) {
            paragraphs.add(new Paragraph(ID + idFromUri, writePdfSettings.textFont));
            addRelations(paragraphs, concept.getRelationsList(), writePdfSettings, labels);
            addDocuments(paragraphs, concept.getDocumentationsList(), traductions.get(idFromUri), codeLanguage1, codeLanguage2, writePdfSettings);
            addMatchs(paragraphs, concept.getMatchList(), writePdfSettings);
            addGpsCoordiantes(paragraphs, concept.getGpsCoordinates(), writePdfSettings);
            if(isToogleExportImage)
                addImages(paragraphs, concept.getNodeImages(), writePdfSettings);
        }
    }

    private boolean addLabels(List<Paragraph> paragraphs, ArrayList<SKOSLabel> labels, String codeLanguage1,
            String codeLanguage2, String idFromUri, String idArk, ThesaurusPdfSettings writePdfSettings, HashMap<String, List<Integer>> traductions) {
        boolean added = false;
        int altLabelCount = 0;
        if (CollectionUtils.isNotEmpty(traductions.get(idFromUri))) {
            altLabelCount = (int) traductions.get(idFromUri).stream().filter(trad -> trad == SKOSProperty.ALT_LABEL).count();
        }

        int altLabelWrite = 0;
        for (SKOSLabel label : labels) {
            if (label.getLanguage().equals(codeLanguage1) || label.getLanguage().equals(codeLanguage2)) {
                added = true;
                String labelValue;
                boolean prefIsTrad = false;
                boolean altIsTrad = false;

                if (label.getLanguage().equals(codeLanguage1)) {
                    labelValue = label.getLabel();
                } else {
                    if (CollectionUtils.isNotEmpty(traductions.get(idFromUri))) {
                        if (traductions.get(idFromUri).contains(SKOSProperty.PREF_LABEL) && label.getProperty() == SKOSProperty.PREF_LABEL) {
                            prefIsTrad = true;
                        }
                        if (traductions.get(idFromUri).contains(SKOSProperty.ALT_LABEL) && label.getProperty() == SKOSProperty.ALT_LABEL) {
                            if (altLabelCount > altLabelWrite) {
                                altIsTrad = true;
                            }
                            altLabelWrite++;
                        }
                    }
                    labelValue = "-";
                }

                if (label.getProperty() == SKOSProperty.PREF_LABEL && !prefIsTrad) {
                    Paragraph paragraph = new Paragraph();
                    Anchor anchor = new Anchor(labelValue, writePdfSettings.termFont);
                    anchor.setReference(uriResolver.getUriForConcept(
                            exportPreferences, exportThesaurusId, idFromUri, idArk, idArk));

                    anchor.setName(idFromUri);

                    paragraph.add(anchor);
                    paragraphs.add(paragraph);
                } else if (label.getProperty() == SKOSProperty.ALT_LABEL && !altIsTrad) {
                    paragraphs.add(new Paragraph(USE + labelValue, writePdfSettings.textFont));
                }
            }
        }
        return added;
    }

    private void addRelations(List<Paragraph> paragraphs, List<SKOSRelation> relations, ThesaurusPdfSettings writePdfSettings, HashMap<String, String> labels) {

        if (CollectionUtils.isNotEmpty(relations)) {
            relations.stream()
                    .forEach(relation -> {
                        switch (relation.getProperty()) {
                    case SKOSProperty.INSCHEME:
                        break;
                    case SKOSProperty.TOP_CONCEPT_OF:
                        break;
                    case SKOSProperty.NARROWER:
                        appendRelation(paragraphs, relation, writePdfSettings, labels);
                        break;
                    case SKOSProperty.BROADER:
                        appendRelation(paragraphs, relation, writePdfSettings, labels);
                        break;
                    case SKOSProperty.RELATED:
                        appendRelation(paragraphs, relation, writePdfSettings, labels);
                        break;                        
                    default:
                        break;
                }
            });

        }
    }

    private void appendRelation(List<Paragraph> paragraphs, SKOSRelation relation, ThesaurusPdfSettings writePdfSettings, HashMap<String, String> labels) {
        String targetName = labels.get(relation.getLocalIdentifier());
        if(!StringUtils.isEmpty(targetName)){
            if (StringUtils.isNotEmpty(writePdfSettings.getCodeRelation(relation.getProperty()))) {
                Chunk chunk = new Chunk(TAB_NIVEAU + writePdfSettings.getCodeRelation(relation.getProperty())
                        + ": " + targetName, writePdfSettings.relationFont);
                chunk.setLocalGoto(relation.getLocalIdentifier());//writePdfSettings.getIdFromUri(relation.getTargetUri()));
                paragraphs.add(new Paragraph(chunk));
            }            
        }
    }

    private void addDocuments(List<Paragraph> paragraphs, List<SKOSDocumentation> documentations, List<Integer> tradList,
            String codeLanguage1, String codeLanguage2, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(documentations)) {
            documentations.stream()
                    .filter(document -> (document.getLanguage().equals(codeLanguage1) || document.getLanguage().equals(codeLanguage2)))
                    .forEach(document -> {
                        int docCount = 0;
                        if (CollectionUtils.isNotEmpty(tradList)) {
                            docCount = (int) tradList.stream().filter(traduction -> traduction == SKOSProperty.NOTE).count();
                        }

                        String docText = "";
                        boolean docIsTrad = false;
                        if (document.getLanguage().equals(codeLanguage1)) {
                            docText = document.getText();
                        } else {
                            if (CollectionUtils.isNotEmpty(tradList) && tradList.contains(SKOSProperty.NOTE)) {
                                docIsTrad = (docCount > 0);
                            }
                        }

                        if (!docIsTrad) {
                            paragraphs.add(new Paragraph(TAB_NIVEAU + writePdfSettings.getDocTypeName(document.getProperty())
                                    + ": " + docText, writePdfSettings.textFont));
                        }
                    });
        }
    }

    private void addMatchs(List<Paragraph> paragraphs, List<SKOSMatch> matchs, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(matchs)) {
            matchs.stream().forEach(match
                    -> paragraphs.add(new Paragraph(TAB_NIVEAU + writePdfSettings.getMatchTypeName(match.getProperty())
                            + ": " + match.getValue(), writePdfSettings.textFont))
            );
        }
    }

    private void addGpsCoordiantes(List<Paragraph> paragraphs, List<SKOSGPSCoordinates> skosGpsCoordinates, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(skosGpsCoordinates)) {
            paragraphs.add(new Paragraph(GPS + formatCoordonnees(skosGpsCoordinates), writePdfSettings.textFont));
        }
    }

    public String formatCoordonnees(List<SKOSGPSCoordinates> listeCoordonnees) {
        if (CollectionUtils.isEmpty(listeCoordonnees)) {
            return "";
        } else {
            StringBuilder resultat = new StringBuilder();
            for (SKOSGPSCoordinates gps : listeCoordonnees) {
                resultat.append(gps.toString()).append(", ");
            }
            return "(" + resultat.substring(0, resultat.length() - 2) + ")";
        }
    }

    private void addImages(List<Paragraph> paragraphs, List<NodeImage> images, ThesaurusPdfSettings writePdfSettings) {
        if (CollectionUtils.isNotEmpty(images)) {
            paragraphs.add(new Paragraph(Chunk.NEWLINE));
            for (NodeImage imageElement : images) {
                if (imageElement.getUri() != null && !imageElement.getUri().isEmpty()) {
                    try {
                        // Téléchargement de l'image
                        URL imageUrl = new URL(imageElement.getUri());
                        Image image = Image.getInstance(imageUrl);

                        // Redimensionnement
                        float scaleFactor = writePdfSettings.resiseImage(image);
                        if (scaleFactor > 0) {
                            image.scaleAbsolute(image.getWidth() / scaleFactor, image.getHeight() / scaleFactor);
                            // Ajout de l'image au paragraphe avec indentation
                            paragraphs.add(new Paragraph(new Chunk(image, 11, 0, true)));
                        } else {
                            paragraphs.add(new Paragraph("Erreur de redimensionnement de l'image : " + imageElement.getUri()));
                        }
                    } catch (BadElementException | IOException ex) {
                        paragraphs.add(new Paragraph("Erreur de téléchargement de l'image (image vide) : " + imageElement.getUri()));
                    }
                } else {
                    paragraphs.add(new Paragraph("Image invalide : " + imageElement.getUri()));
                }
            }
        }
    }

    /*
    private void addImages(List<Paragraph> paragraphs, List<NodeImage> images, ThesaurusPdfSettings writePdfSettings) {

        if (CollectionUtils.isNotEmpty(images)) {
            paragraphs.add(new Paragraph(Chunk.NEWLINE));
            images.stream().forEach(element -> {
                try {
                    Image image = Image.getInstance(new URL(element.getUri()));
                    image.scaleAbsolute(writePdfSettings.resiseImage(image));
                    paragraphs.add(new Paragraph(new Chunk(image, 11, 0, true)));
                } catch (Exception ex) {
                }
            });
        }
    }*/
}
