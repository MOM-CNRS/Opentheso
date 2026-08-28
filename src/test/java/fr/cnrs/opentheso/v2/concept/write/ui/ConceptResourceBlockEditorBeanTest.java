package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptMediaMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptResourceBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptMediaMutationService conceptMediaMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptResourceBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptResourceBlockEditorBean(
                thesaurusViewBean,
                conceptMediaMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateMedia(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditingLinks_copiesOnlyResources() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(fullDetail());

        bean.startEditingLinks();

        assertTrue(bean.isEditingLinks());
        assertFalse(bean.isEditingImages());
        assertFalse(bean.isEditingGps());
        assertEquals(1, bean.getResourceRows().size());
        assertEquals("https://example.org/a", bean.getResourceRows().get(0).getUri());
        assertTrue(bean.getImageRows().isEmpty());
        assertTrue(bean.getGpsRows().isEmpty());
        assertEquals("v2.concept.resource.links.confirmTitle", bean.getConfirmTitleKey());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void startEditingImages_copiesOnlyImages() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(fullDetail());

        bean.startEditingImages();

        assertTrue(bean.isEditingImages());
        assertEquals(1, bean.getImageRows().size());
        assertEquals(4, bean.getImageRows().get(0).getId());
        assertTrue(bean.getResourceRows().isEmpty());
        assertTrue(bean.getGpsRows().isEmpty());
        assertEquals("v2.concept.resource.images.confirm", bean.getConfirmMessageKey());
    }

    @Test
    void startEditingGps_copiesOnlyCoordinates() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(fullDetail());

        bean.startEditingGps();

        assertTrue(bean.isEditingGps());
        assertEquals(1, bean.getGpsRows().size());
        assertEquals("48.85", bean.getGpsRows().get(0).getLatitude());
        assertTrue(bean.getResourceRows().isEmpty());
        assertTrue(bean.getImageRows().isEmpty());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateMedia(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));

        bean.startEditingLinks();
        bean.save();

        verify(conceptMediaMutationService, never()).addExternalResource(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));
        bean.startEditingLinks();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                "C2", List.of(), List.of(), List.of()));

        assertFalse(bean.isEditing());
    }

    @Test
    void save_updatesLinksOnly() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(
                        new ConceptExternalResourceItem("https://example.org/old", "Ancien"),
                        new ConceptExternalResourceItem("https://example.org/keep", "Keep")
                ),
                List.of(new ConceptImageItem(4, "Photo", "CC", "Ada", "https://example.org/old.jpg")),
                List.of(new ConceptGpsPoint(48.85, 2.35, 1))
        ));
        when(conceptMediaMutationService.deleteExternalResource(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptMediaMutationService.addExternalResource(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptMediaMutationService.updateExternalResource(any())).thenReturn(MutationResult.ok("ok"));

        bean.startEditingLinks();
        bean.getResourceRows().get(0).setUri("https://example.org/new");
        bean.getResourceRows().get(0).setDescription("Nouveau");
        bean.removeResourceRow(1);
        bean.addResourceRow();
        bean.getResourceRows().get(1).setUri("https://example.org/added");

        bean.save();

        ArgumentCaptor<DeleteExternalResourceCommand> deletedRes = ArgumentCaptor.forClass(DeleteExternalResourceCommand.class);
        verify(conceptMediaMutationService).deleteExternalResource(deletedRes.capture());
        assertEquals("https://example.org/keep", deletedRes.getValue().uri());

        ArgumentCaptor<UpdateExternalResourceCommand> updatedRes = ArgumentCaptor.forClass(UpdateExternalResourceCommand.class);
        verify(conceptMediaMutationService).updateExternalResource(updatedRes.capture());
        assertEquals("https://example.org/old", updatedRes.getValue().oldUri());
        assertEquals("https://example.org/new", updatedRes.getValue().uri());

        ArgumentCaptor<AddExternalResourceCommand> addedRes = ArgumentCaptor.forClass(AddExternalResourceCommand.class);
        verify(conceptMediaMutationService).addExternalResource(addedRes.capture());
        assertEquals("https://example.org/added", addedRes.getValue().uri());

        verify(conceptMediaMutationService, never()).replaceGpsCoordinates(any());
        verify(conceptMediaMutationService, never()).addImage(any());
        assertFalse(bean.isEditing());
        assertEquals("Liens enregistrés", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    @Test
    void save_updatesImagesOnly() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptExternalResourceItem("https://example.org/a", "A")),
                List.of(
                        new ConceptImageItem(4, "Photo", "CC", "Ada", "https://example.org/old.jpg"),
                        new ConceptImageItem(5, "Keep", "", "", "https://example.org/keep.jpg")
                ),
                List.of(new ConceptGpsPoint(48.85, 2.35, 1))
        ));
        when(conceptMediaMutationService.deleteImage(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptMediaMutationService.addImage(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptMediaMutationService.updateImage(any())).thenReturn(MutationResult.ok("ok"));

        bean.startEditingImages();
        bean.getImageRows().get(0).setName("Photo 2");
        bean.removeImageRow(1);
        bean.addImageRow();
        bean.getImageRows().get(1).setUri("https://example.org/new.jpg");

        bean.save();

        ArgumentCaptor<DeleteConceptImageCommand> deletedImage = ArgumentCaptor.forClass(DeleteConceptImageCommand.class);
        verify(conceptMediaMutationService).deleteImage(deletedImage.capture());
        assertEquals("https://example.org/keep.jpg", deletedImage.getValue().uri());

        ArgumentCaptor<UpdateConceptImageCommand> updatedImage = ArgumentCaptor.forClass(UpdateConceptImageCommand.class);
        verify(conceptMediaMutationService).updateImage(updatedImage.capture());
        assertEquals(4, updatedImage.getValue().imageId());
        assertEquals("Photo 2", updatedImage.getValue().name());

        ArgumentCaptor<AddConceptImageCommand> addedImage = ArgumentCaptor.forClass(AddConceptImageCommand.class);
        verify(conceptMediaMutationService).addImage(addedImage.capture());
        assertEquals("https://example.org/new.jpg", addedImage.getValue().uri());

        verify(conceptMediaMutationService, never()).replaceGpsCoordinates(any());
        verify(conceptMediaMutationService, never()).addExternalResource(any());
        assertEquals("Images enregistrées", bean.getFlashMessage());
    }

    @Test
    void save_updatesGpsOnly() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptExternalResourceItem("https://example.org/a", "A")),
                List.of(new ConceptImageItem(4, "Photo", "CC", "Ada", "https://example.org/old.jpg")),
                List.of(new ConceptGpsPoint(48.85, 2.35, 1))
        ));
        when(conceptMediaMutationService.replaceGpsCoordinates(any())).thenReturn(MutationResult.ok("ok"));

        bean.startEditingGps();
        bean.getGpsRows().get(0).setLatitude("48.9");
        bean.getGpsRows().get(0).setLongitude("2.4");

        bean.save();

        ArgumentCaptor<ReplaceGpsCoordinatesCommand> gps = ArgumentCaptor.forClass(ReplaceGpsCoordinatesCommand.class);
        verify(conceptMediaMutationService).replaceGpsCoordinates(gps.capture());
        assertEquals("(48.9 2.4)", gps.getValue().coordinatesText());
        verify(conceptMediaMutationService, never()).addImage(any());
        verify(conceptMediaMutationService, never()).addExternalResource(any());
        assertEquals("Coordonnées GPS enregistrées", bean.getFlashMessage());
    }

    @Test
    void closePolygon_appendsFirstPoint() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(),
                List.of(),
                List.of(
                        new ConceptGpsPoint(48.81, 2.22, 1),
                        new ConceptGpsPoint(48.81, 2.43, 2),
                        new ConceptGpsPoint(48.91, 2.34, 3)
                )
        ));
        bean.startEditingGps();
        assertTrue(bean.isCanClosePolygon());

        bean.closePolygon();

        assertEquals(4, bean.getGpsRows().size());
        assertEquals(bean.getGpsRows().get(0).getLatitude(), bean.getGpsRows().get(3).getLatitude());
        assertEquals(bean.getGpsRows().get(0).getLongitude(), bean.getGpsRows().get(3).getLongitude());
        assertFalse(bean.isCanClosePolygon());
    }

    @Test
    void addGpsAtClick_appendsNormalizedPoint() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));
        bean.startEditingGps();
        bean.setClickLatitude("48.8566");
        bean.setClickLongitude("2.3522");

        bean.addGpsAtClick();

        assertEquals(1, bean.getGpsRows().size());
        assertEquals("48.8566", bean.getGpsRows().get(0).getLatitude());
        assertEquals("2.3522", bean.getGpsRows().get(0).getLongitude());
        assertEquals("", bean.getClickLatitude());
        assertEquals("", bean.getClickLongitude());
    }

    @Test
    void save_rejectsInvalidResourceUrl() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));
        bean.startEditingLinks();
        bean.addResourceRow();
        bean.getResourceRows().get(0).setUri("not-a-url");

        bean.save();

        assertEquals("L'URL n'est pas valide !", bean.getErrorMessage());
        verify(conceptMediaMutationService, never()).addExternalResource(any());
        assertTrue(bean.isEditing());
    }

    private ConceptDetail fullDetail() {
        return detail(
                List.of(new ConceptExternalResourceItem("https://example.org/a", "Site A")),
                List.of(new ConceptImageItem(4, "Photo", "CC-BY", "Ada", "https://example.org/p.jpg")),
                List.of(new ConceptGpsPoint(48.85, 2.35, 1))
        );
    }

    private static ConceptDetail detail(
            List<ConceptExternalResourceItem> resources,
            List<ConceptImageItem> images,
            List<ConceptGpsPoint> gps
    ) {
        return detail("C1", resources, images, gps);
    }

    private static ConceptDetail detail(
            String id,
            List<ConceptExternalResourceItem> resources,
            List<ConceptImageItem> images,
            List<ConceptGpsPoint> gps
    ) {
        var summary = new ConceptSummary(id, "TH1", "Chat", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()),
                images,
                gps,
                resources,
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }
}
