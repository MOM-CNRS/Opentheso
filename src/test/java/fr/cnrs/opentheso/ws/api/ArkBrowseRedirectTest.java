package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.ThesaurusService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkBrowseRedirectTest {

    @Test
    void arkPath_matchesLocalApiThesaurus() {
        var matcher = ArkBrowseRedirectFilter.ARK_PATH.matcher(
                "/api/ark:/55555/df-wl1h0lrqjw621hbc8v0t-4");

        assertTrue(matcher.matches());
        assertEquals("55555", matcher.group(1));
        assertEquals("df-wl1h0lrqjw621hbc8v0t-4", matcher.group(2));
    }

    @Test
    void localBrowsePath_opensThesaurus() {
        assertEquals("/idt=th217", RestRDFHelper.localBrowsePath("th217", null, null));
    }

    @Test
    void getUrlFromIdArk_resolvesThesaurus() {
        ConceptService concepts = mock(ConceptService.class);
        ThesaurusService thesauri = mock(ThesaurusService.class);
        GroupService groups = mock(GroupService.class);
        when(concepts.getIdThesaurusFromArkId("55555/df-wl1h0lrqjw621hbc8v0t-4")).thenReturn(null);
        when(thesauri.getIdThesaurusFromArkId("55555/df-wl1h0lrqjw621hbc8v0t-4")).thenReturn("th217");

        RestRDFHelper helper = new RestRDFHelper(
                null, thesauri, null, null, null, null, null, null, groups,
                null, concepts, null, null, null, null);

        assertEquals("/idt=th217", helper.getUrlFromIdArk("55555", "df-wl1h0lrqjw621hbc8v0t-4"));
    }
}
