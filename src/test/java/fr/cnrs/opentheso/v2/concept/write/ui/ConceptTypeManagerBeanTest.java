package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTypeManagerBeanTest {

    @Mock
    private ConceptTypeService conceptTypeService;
    @Mock
    private ConceptTypeRepository conceptTypeRepository;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private ThesaurusBrowseBean thesaurusBrowseBean;

    private ConceptTypeManagerBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptTypeManagerBean(
                conceptTypeService,
                conceptTypeRepository,
                thesaurusContext,
                userSession,
                conceptWritePolicy,
                thesaurusBrowseBean
        );
        lenient().when(conceptWritePolicy.canMutateConcept(userSession)).thenReturn(true);
        lenient().when(thesaurusBrowseBean.isCustomRelationVisible()).thenReturn(true);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
    }

    @Test
    void prepareManage_loadsSystemAndCustomTypes() {
        when(conceptTypeRepository.findAllByIdThesaurusIn(List.of("TH1", "all")))
                .thenReturn(List.of(
                        type("concept", "all", "Concept", "Concept", false),
                        type("lieu", "TH1", "Lieu", "Place", true)
                ));

        bean.prepareManage();

        assertEquals(2, bean.getConceptTypes().size());
        assertTrue(bean.getConceptTypes().get(0).isPermanent());
        assertFalse(bean.getConceptTypes().get(1).isPermanent());
        assertTrue(bean.getConceptTypes().get(1).isReciprocal());
        assertFalse(bean.isDirty());
        assertNull(bean.getErrorMessage());
    }

    @Test
    void applyChange_rejectsPermanentType() {
        NodeConceptType type = NodeConceptType.builder()
                .code("concept")
                .permanent(true)
                .labelFr("Concept")
                .build();

        bean.applyChange(type);

        assertEquals("Ce type système n'est pas modifiable", bean.getErrorMessage());
        verify(conceptTypeService, never()).updateConceptType(any(), any());
        assertFalse(bean.isDirty());
    }

    @Test
    void applyChange_rejectsEmptyLabels() {
        NodeConceptType type = NodeConceptType.builder().code("lieu").labelFr(" ").labelEn("").build();

        bean.applyChange(type);

        assertEquals("Indiquez au moins un libellé", bean.getErrorMessage());
        verify(conceptTypeService, never()).updateConceptType(any(), any());
    }

    @Test
    void addNewConceptType_rejectsDuplicateCode() {
        when(conceptTypeRepository.findAllByIdThesaurusIn(any()))
                .thenReturn(List.of(type("lieu", "TH1", "Lieu", "Place", false)));
        bean.prepareManage();
        bean.getConceptTypeToAdd().setCode("Lieu");
        bean.getConceptTypeToAdd().setLabelFr("Lieu");

        bean.addNewConceptType();

        assertEquals("Le type « lieu » existe déjà", bean.getErrorMessage());
        verify(conceptTypeService, never()).addNewConceptType(any(), any());
    }

    @Test
    void addNewConceptType_normalizesCodeAndAdds() {
        when(conceptTypeRepository.findAllByIdThesaurusIn(any())).thenReturn(List.of());
        when(conceptTypeService.isConceptTypeExist(eq("TH1"), any())).thenReturn(false);
        bean.prepareManage();
        bean.getConceptTypeToAdd().setCode(" Mon Type ");
        bean.getConceptTypeToAdd().setLabelFr("Mon type");

        bean.addNewConceptType();

        assertTrue(bean.isDirty());
        assertTrue(bean.getFlashMessage().contains("montype"));
        assertNull(bean.getConceptTypeToAdd().getCode());
        verify(conceptTypeService).addNewConceptType(eq("TH1"), argThat(type -> "montype".equals(type.getCode())));
    }

    @Test
    void applyChange_updatesCustomType() {
        NodeConceptType type = NodeConceptType.builder()
                .code("lieu")
                .labelFr("Lieu")
                .labelEn("Place")
                .build();
        when(conceptTypeService.updateConceptType("TH1", type)).thenReturn(true);
        when(conceptTypeRepository.findAllByIdThesaurusIn(any()))
                .thenReturn(List.of(type("lieu", "TH1", "Lieu", "Place", false)));

        bean.applyChange(type);

        assertTrue(bean.isDirty());
        assertTrue(bean.getFlashMessage().contains("lieu"));
        assertNull(bean.getErrorMessage());
        verify(conceptTypeService).updateConceptType("TH1", type);
    }

    @Test
    void delete_requiresConfirmThenRemoves() {
        NodeConceptType type = NodeConceptType.builder().code("lieu").permanent(false).build();
        when(conceptTypeRepository.findAllByIdThesaurusIn(any())).thenReturn(List.of());

        bean.prepareDelete(type);
        assertTrue(bean.isPendingDelete(type));

        bean.deleteCustomRelationship();

        verify(conceptTypeService).deleteConceptType("TH1", type);
        assertFalse(bean.isPendingDelete(type));
        assertTrue(bean.isDirty());
        assertTrue(bean.getFlashMessage().contains("lieu"));
    }

    @Test
    void prepareDelete_rejectsPermanentType() {
        NodeConceptType type = NodeConceptType.builder().code("concept").permanent(true).build();

        bean.prepareDelete(type);

        assertEquals("Ce type ne peut pas être supprimé", bean.getErrorMessage());
        assertFalse(bean.isPendingDelete(type));
    }

    private static ConceptType type(String code, String thesaurus, String fr, String en, boolean reciprocal) {
        return ConceptType.builder()
                .code(code)
                .idThesaurus(thesaurus)
                .labelFr(fr)
                .labelEn(en)
                .reciprocal(reciprocal)
                .build();
    }
}
