package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockIds;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import fr.cnrs.opentheso.v2.user.service.ConceptBlockLayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptBlockLayoutAccountBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean localeBean;
    @Mock
    private ConceptBlockLayoutService conceptBlockLayoutService;

    private ConceptBlockLayoutAccountBean bean;

    @BeforeEach
    void setUp() {
        lenient().when(localeBean.getMsg(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        bean = new ConceptBlockLayoutAccountBean(userSession, localeBean, conceptBlockLayoutService);
    }

    @Test
    void load_usesDefaultsWhenAnonymous() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.load();

        assertEquals(ConceptBlockIds.DEFAULT_ORDER, bean.getRows().stream().map(ConceptBlockLayoutRow::getId).toList());
        assertTrue(bean.getRows().stream().allMatch(ConceptBlockLayoutRow::isOpen));
        verifyNoInteractions(conceptBlockLayoutService);
    }

    @Test
    void load_mapsSavedLayout() {
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(conceptBlockLayoutService.getLayout(4)).thenReturn(
                new ConceptBlockLayout(List.of("notes", "contexte"), Set.of("notes"))
        );

        bean.load();

        assertEquals("notes", bean.getRows().get(0).getId());
        assertFalse(bean.getRows().get(0).isOpen());
        assertEquals("contexte", bean.getRows().get(1).getId());
        assertTrue(bean.getRows().get(1).isOpen());
        assertEquals(ConceptBlockIds.DEFAULT_ORDER.size(), bean.getRows().size());
    }

    @Test
    void moveUp_persistsSwappedOrder() {
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(conceptBlockLayoutService.getLayout(4)).thenReturn(ConceptBlockLayout.defaults());
        when(conceptBlockLayoutService.saveLayout(anyInt(), anyList(), anyList()))
                .thenReturn(ConceptBlockLayout.defaults());
        bean.load();

        bean.moveUp("collections");

        verify(conceptBlockLayoutService).saveLayout(
                4,
                List.of("collections", "contexte", "relations", "relPerso", "traductions",
                        "notes", "ressources", "alignement", "identifiants", "temporel"),
                List.of()
        );
    }

    @Test
    void toggleOpen_persistsCollapsedId() {
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(conceptBlockLayoutService.getLayout(4)).thenReturn(ConceptBlockLayout.defaults());
        when(conceptBlockLayoutService.saveLayout(anyInt(), anyList(), anyList()))
                .thenReturn(new ConceptBlockLayout(ConceptBlockIds.DEFAULT_ORDER, Set.of("notes")));
        bean.load();

        bean.toggleOpen("notes");

        verify(conceptBlockLayoutService).saveLayout(4, ConceptBlockIds.DEFAULT_ORDER, List.of("notes"));
    }

    @Test
    void resetDefault_deletesPrefs() {
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(conceptBlockLayoutService.resetLayout(4)).thenReturn(ConceptBlockLayout.defaults());

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.resetDefault();
            messages.verify(() -> MessageUtils.showInformationMessage("v2.profile.blocks.resetDone"));
        }

        verify(conceptBlockLayoutService).resetLayout(4);
        assertEquals(ConceptBlockIds.DEFAULT_ORDER.size(), bean.getRows().size());
        assertTrue(bean.getRows().stream().allMatch(ConceptBlockLayoutRow::isOpen));
    }

    @Test
    void getRows_loadsLazilyWhenInitWasSkipped() {
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(conceptBlockLayoutService.getLayout(4)).thenReturn(ConceptBlockLayout.defaults());

        assertEquals(ConceptBlockIds.DEFAULT_ORDER.size(), bean.getRows().size());
        assertEquals("contexte", bean.getRows().get(0).getId());
    }
}
