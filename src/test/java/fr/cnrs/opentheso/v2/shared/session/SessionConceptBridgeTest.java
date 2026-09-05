package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionConceptTreeRefreshSupportTest {

    private ConceptTreeRefreshState refreshState;

    @Mock
    private ObjectProvider<ConceptNavigationSupport> conceptNavigationSupportProvider;
    @Mock
    private ConceptNavigationSupport conceptNavigationSupport;

    private SessionConceptTreeRefreshSupport support;

    @BeforeEach
    void setUp() {
        refreshState = new ConceptTreeRefreshState();
        support = new SessionConceptTreeRefreshSupport(refreshState, conceptNavigationSupportProvider);
    }

    @Test
    void refreshConceptTree_marksPendingWhenBrowseUnavailable() {
        when(conceptNavigationSupportProvider.getIfAvailable()).thenReturn(null);

        support.refreshConceptTree();

        assertTrue(refreshState.consumeRefresh());
    }

    @Test
    void refreshConceptTree_invalidatesBrowseWhenAvailable() {
        when(conceptNavigationSupportProvider.getIfAvailable()).thenReturn(conceptNavigationSupport);

        support.refreshConceptTree();

        verify(conceptNavigationSupport).invalidateConceptTree();
        assertFalse(refreshState.consumeRefresh());
    }
}

@ExtendWith(MockitoExtension.class)
class SessionConceptSelectionSourceTest {

    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private SessionConceptSelectionSource source;

    @BeforeEach
    void setUp() {
        source = new SessionConceptSelectionSource(conceptSelectionContext);
    }

    @Test
    void getSelectedConceptId_returnsBlankWhenMissing() {
        when(conceptSelectionContext.getConceptId()).thenReturn("  ");

        assertTrue(source.getSelectedConceptId().isEmpty());
    }

    @Test
    void getSelectedConceptId_returnsConceptFromContext() {
        when(conceptSelectionContext.getConceptId()).thenReturn("C1");

        Optional<String> conceptId = source.getSelectedConceptId();

        assertEquals("C1", conceptId.orElseThrow());
    }
}
