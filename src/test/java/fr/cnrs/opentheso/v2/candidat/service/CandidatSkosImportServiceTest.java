package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.legacybridge.LegacyThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.candidat.session.CandidatSkosImportLegacySupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CandidatSkosImportServiceTest {

    @Mock
    private CandidatSkosImportLegacySupport candidatSkosImportLegacySupport;
    @Mock
    private LegacyThesaurusPreferencesProvider legacyThesaurusPreferencesProvider;

    private CandidatSkosImportService service;

    @BeforeEach
    void setUp() {
        service = new CandidatSkosImportService(candidatSkosImportLegacySupport, legacyThesaurusPreferencesProvider);
    }

    @Test
    void importCandidates_addsOnlyConceptsWithLabels() throws Exception {
        var document = new SKOSXmlDocument();
        var withLabel = new SKOSResource();
        withLabel.getLabelsList().add(new SKOSLabel("chat", "fr", SKOSProperty.PREF_LABEL));
        var withoutLabel = new SKOSResource();
        var conceptList = new ArrayList<SKOSResource>();
        conceptList.add(withLabel);
        conceptList.add(withoutLabel);
        document.setConceptList(conceptList);

        var preferences = new Preferences();
        preferences.setSourceLang("fr");
        var progress = new AtomicInteger();

        service.importCandidates(
                document,
                "TH1",
                7,
                -1,
                "fr",
                preferences,
                (current, total) -> progress.set(current * 100 / total)
        );

        verify(candidatSkosImportLegacySupport).configureImport(eq("yyyy-MM-dd"), eq(7), eq(-1), eq("fr"), eq(preferences));
        verify(candidatSkosImportLegacySupport).setImportDocument(document);
        verify(candidatSkosImportLegacySupport).importConcept(withLabel, "TH1", true);
        assertEquals(100, progress.get());
    }
}
