package fr.cnrs.opentheso.v2.publicapi.group.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupPublicExportServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private RelationGroupRepository relationGroupRepository;
    @Mock
    private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private GroupPublicExportService service;

    @BeforeEach
    void setUp() {
        service = new GroupPublicExportService(
                thesaurusSkosDocumentBuilder,
                conceptSkosRdfExportEngine,
                relationGroupRepository,
                conceptGroupLabelRepository,
                conceptGroupRepository,
                thesaurusWorkLanguageService
        );
    }

    @Test
    void exportGroup_buildsAndSerializes() throws Exception {
        when(thesaurusSkosDocumentBuilder.buildDocumentByGroup("TH1", "G1", false)).thenReturn(new SKOSXmlDocument());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{1});

        var result = service.exportGroup("TH1", "G1", "skos");

        assertEquals("TH1_G1.rdf", result.filename());
    }

    @Test
    void exportGroup_wrapsBuildFailureAsIllegalState() throws Exception {
        when(thesaurusSkosDocumentBuilder.buildDocumentByGroup("TH1", "G1", false))
                .thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> service.exportGroup("TH1", "G1", "skos"));
    }

    @Test
    void exportBranch_buildsDocumentWithSelectedGroups() throws Exception {
        when(thesaurusSkosDocumentBuilder.buildDocument(eq("TH1"), any(ThesaurusEditionExportOptions.class)))
                .thenReturn(new SKOSXmlDocument());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{2});

        var result = service.exportBranch("TH1", List.of("G1", "G2"), "turtle");

        assertEquals("TH1_branch.ttl", result.filename());
    }

    @Test
    void branchTree_walksParentChainUpToRoot() {
        var parentRelation = RelationGroup.builder().idGroup1("G0").idGroup2("G1").idThesaurus("TH1").relation("sub").build();
        when(relationGroupRepository.findByIdThesaurusAndIdGroup2AndRelation("TH1", "G1", "sub"))
                .thenReturn(Optional.of(parentRelation));
        when(relationGroupRepository.findByIdThesaurusAndIdGroup2AndRelation("TH1", "G0", "sub"))
                .thenReturn(Optional.empty());
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang("TH1", "G1", "fr"))
                .thenReturn(List.of(ConceptGroupLabel.builder().idGroup("G1").lang("fr").lexicalValue("Groupe 1").build()));
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang("TH1", "G0", "fr"))
                .thenReturn(List.of(ConceptGroupLabel.builder().idGroup("G0").lang("fr").lexicalValue("Groupe racine").build()));

        var response = service.branchTree("TH1", List.of("G1"), "fr");

        assertEquals(1, response.size());
        assertEquals("Groupe 1", response.get(0).label());
        assertEquals(1, response.get(0).pathToRoot().size());
        assertEquals("G0", response.get(0).pathToRoot().get(0).groupId());
        assertEquals("Groupe racine", response.get(0).pathToRoot().get(0).label());
    }

    @Test
    void branchTree_resolvesDefaultLanguageWhenBlank() {
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(relationGroupRepository.findByIdThesaurusAndIdGroup2AndRelation("TH1", "G1", "sub")).thenReturn(Optional.empty());
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang("TH1", "G1", "fr")).thenReturn(List.of());

        var response = service.branchTree("TH1", List.of("G1"), null);

        assertEquals("G1", response.get(0).label());
    }

    @Test
    void listGroups_mapsAllGroupsWithTranslations() {
        when(conceptGroupRepository.findAllByIdThesaurus("TH1"))
                .thenReturn(List.of(ConceptGroup.builder().idGroup("G1").build()));
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroup("TH1", "G1"))
                .thenReturn(List.of(ConceptGroupLabel.builder().idGroup("G1").lang("fr").lexicalValue("Groupe 1").build()));

        var response = service.listGroups("TH1");

        assertEquals(1, response.size());
        assertEquals("G1", response.get(0).groupId());
        assertEquals(1, response.get(0).labels().size());
        assertEquals("fr", response.get(0).labels().get(0).lang());
        assertEquals("Groupe 1", response.get(0).labels().get(0).title());
    }

    @Test
    void listSubGroups_mapsChildGroupsWithTranslations() {
        when(relationGroupRepository.findChildGroupIds("TH1", "G1")).thenReturn(List.of("G2"));
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroup("TH1", "G2"))
                .thenReturn(List.of(ConceptGroupLabel.builder().idGroup("G2").lang("en").lexicalValue("Group 2").build()));

        var response = service.listSubGroups("TH1", "G1");

        assertEquals(1, response.size());
        assertEquals("G2", response.get(0).groupId());
        assertEquals("en", response.get(0).labels().get(0).lang());
        assertEquals("Group 2", response.get(0).labels().get(0).title());
    }
}
