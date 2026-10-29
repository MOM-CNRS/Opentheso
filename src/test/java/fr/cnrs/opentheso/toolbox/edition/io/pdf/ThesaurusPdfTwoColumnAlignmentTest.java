package fr.cnrs.opentheso.toolbox.edition.io.pdf;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.terms.ConceptPreferredTermLookup;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.toolbox.edition.persistence.ThesaurusEditionPdfUriResolver;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThesaurusPdfTwoColumnAlignmentTest {

    @Test
    void hierarchicalColumnsKeepFollowingConceptsOnTheSameRow() throws Exception {
        SKOSXmlDocument document = scheme();
        SKOSResource first = concept("C1",
                "Architecture monumentale du bassin méditerranéen",
                "Architecture");
        first.getDocumentationsList().add(new SKOSDocumentation("note un", "fr", SKOSProperty.NOTE));
        first.getDocumentationsList().add(new SKOSDocumentation("note deux", "fr", SKOSProperty.NOTE));
        first.getDocumentationsList().add(new SKOSDocumentation("note one", "en", SKOSProperty.NOTE));
        document.addconcept(first);
        document.addconcept(concept("C2", "Chien", "Dog"));

        assertSameRow(writer().createPdfFile(document, "fr", "en", ThesaurusPdfExportType.HIERARCHIQUE, false),
                "Chien", "Dog");
    }

    @Test
    void alphabeticColumnsKeepFollowingConceptsOnTheSameRow() throws Exception {
        SKOSXmlDocument document = scheme();
        SKOSResource first = concept("C1", "Pomme", "Apple");
        first.getLabelsList().add(new SKOSLabel("Malus", "fr", SKOSProperty.ALT_LABEL));
        first.getDocumentationsList().add(new SKOSDocumentation("jus", "fr", SKOSProperty.NOTE));
        first.getRelationsList().add(new SKOSRelation("C9", "https://example.test/?idc=C9", SKOSProperty.RELATED));
        document.addconcept(first);
        document.addconcept(concept("C9", "Cible", null));
        document.addconcept(concept("C2", "Poire", "Pear"));

        assertSameRow(writer().createPdfFile(document, "fr", "en", ThesaurusPdfExportType.ALPHABETIQUE, false),
                "Poire", "Pear");
    }

    private static void assertSameRow(byte[] pdf, String leftText, String rightText) throws Exception {
        List<Hit> hits = hits(pdf);
        Hit left = hitContaining(hits, leftText);
        Hit right = hitContaining(hits, rightText);
        assertTrue(left != null, leftText);
        assertTrue(right != null, rightText);
        assertEquals(left.row, right.row, 1.5f,
                leftText + " row=" + left.row + " / " + rightText + " row=" + right.row);
        assertTrue(Math.abs(left.column - right.column) > 40f, "les deux langues doivent rester sur deux colonnes");
    }

    private static Hit hitContaining(List<Hit> hits, String text) {
        Hit found = null;
        for (Hit hit : hits) {
            if (hit.text.contains(text)) {
                found = hit;
            }
        }
        return found;
    }

    private static List<Hit> hits(byte[] pdf) throws Exception {
        List<Hit> hits = new ArrayList<>();
        PdfReader reader = new PdfReader(pdf);
        new PdfReaderContentParser(reader).processContent(1, new RenderListener() {
            @Override
            public void beginTextBlock() {
            }

            @Override
            public void renderText(TextRenderInfo info) {
                var start = info.getBaseline().getStartPoint();
                var end = info.getBaseline().getEndPoint();
                float dx = end.get(0) - start.get(0);
                float dy = end.get(1) - start.get(1);
                boolean vertical = Math.abs(dy) > Math.abs(dx);
                hits.add(new Hit(
                        vertical ? start.get(0) : start.get(1),
                        vertical ? start.get(1) : start.get(0),
                        info.getText()));
            }

            @Override
            public void endTextBlock() {
            }

            @Override
            public void renderImage(ImageRenderInfo renderInfo) {
            }
        });
        reader.close();
        return hits;
    }

    private static ThesaurusPdfWriter writer() {
        ThesaurusEditionPdfUriResolver uriResolver = mock(ThesaurusEditionPdfUriResolver.class);
        when(uriResolver.getUriForConcept(any(), any(), any(), any(), any())).thenReturn("https://example.test/c");
        when(uriResolver.getIdArk(any(), any(), any())).thenReturn("");
        ToolboxPreferencePersistence preferences = mock(ToolboxPreferencePersistence.class);
        when(preferences.findPreferences(anyString())).thenReturn(null);
        return new ThesaurusPdfWriter(
                new ThesaurusPdfHierarchicalWriter(uriResolver, mock(ConceptPreferredTermLookup.class), preferences),
                new ThesaurusPdfAlphabeticWriter(uriResolver, preferences));
    }

    private static SKOSXmlDocument scheme() throws Exception {
        SKOSXmlDocument document = new SKOSXmlDocument();
        SKOSResource scheme = new SKOSResource("https://example.test/?idt=TH1", SKOSProperty.CONCEPT_SCHEME);
        scheme.getLabelsList().add(new SKOSLabel("Thésaurus", "fr", SKOSProperty.PREF_LABEL));
        scheme.getLabelsList().add(new SKOSLabel("Thesaurus", "en", SKOSProperty.PREF_LABEL));
        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setId_thesaurus("TH1");
        thesaurus.setTitle("Thésaurus");
        scheme.setThesaurus(thesaurus);
        document.setConceptScheme(scheme);
        return document;
    }

    private static SKOSResource concept(String id, String french, String english) throws Exception {
        SKOSResource concept = new SKOSResource("https://example.test/?idc=" + id + "&idt=TH1", SKOSProperty.CONCEPT);
        concept.setIdentifier(id);
        concept.setLocalUri("https://example.test/?idc=" + id + "&idt=TH1");
        if (french != null) {
            concept.getLabelsList().add(new SKOSLabel(french, "fr", SKOSProperty.PREF_LABEL));
        }
        if (english != null) {
            concept.getLabelsList().add(new SKOSLabel(english, "en", SKOSProperty.PREF_LABEL));
        }
        return concept;
    }

    private record Hit(float row, float column, String text) {
    }
}
