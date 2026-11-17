package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.model.PickerFooterLink;
import fr.cnrs.opentheso.shared.model.PickerFooterSettings;
import fr.cnrs.opentheso.shared.repository.PickerFooterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickerFooterServiceTest {

    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 0
    };

    @Mock
    private PickerFooterRepository repository;

    private PickerFooterService service;

    @BeforeEach
    void setUp() {
        service = new PickerFooterService(repository);
    }

    @Test
    void load_usesTheMinistryFooterWhenNothingIsStored() {
        when(repository.findSettings()).thenReturn(Optional.empty());

        PickerFooterSettings settings = service.load();

        assertEquals(PickerFooterService.DEFAULT_BRAND_LABEL, settings.brandLabel());
        assertEquals(PickerFooterService.DEFAULT_BRAND_URL, settings.brandUrl());
        assertEquals(4, settings.links().size());
        assertEquals("legifrance.gouv.fr", settings.links().get(0).label());
        assertFalse(settings.customImage());
        verify(repository, never()).findLinks();
    }

    @Test
    void load_keepsAnEmptyLinkListOnceTheFooterExists() {
        when(repository.findSettings()).thenReturn(Optional.of(
                new PickerFooterSettings("Musée", "https://example.org", false, 0L, List.of())));
        when(repository.findLinks()).thenReturn(List.of());

        PickerFooterSettings settings = service.load();

        assertTrue(settings.links().isEmpty());
        assertEquals("Musée", service.load().brandLabel());
        verify(repository, times(1)).findSettings();
    }

    @Test
    void save_replacesLinksAndKeepsTheCurrentLogo() {
        when(repository.findSettings()).thenReturn(Optional.of(
                new PickerFooterSettings("Ministère de la Culture", "https://www.culture.gouv.fr", true, 8L, List.of())));

        String payload = PickerFooterService.encode(List.of(
                new PickerFooterLink("  data.gouv.fr  ", "https://www.data.gouv.fr")));

        assertEquals(PickerFooterService.SaveResult.SAVED,
                service.save(" Ministère ", "https://www.culture.gouv.fr", payload, null, false));

        verify(repository).updateBrand("Ministère", "https://www.culture.gouv.fr");
        verify(repository).replaceLinks(List.of(new PickerFooterLink("data.gouv.fr", "https://www.data.gouv.fr")));
        verify(repository, never()).replaceImage(anyString(), anyString(), any(), anyString(), anyLong());
        assertEquals(1, service.load().links().size());
        assertTrue(service.load().customImage());
    }

    @Test
    void save_acceptsNoLinks() {
        assertEquals(PickerFooterService.SaveResult.SAVED,
                service.save("Logo", "https://example.org", "", null, false));

        verify(repository).replaceLinks(List.of());
    }

    @Test
    void save_rejectsAScriptUrlAndABrokenLink() {
        assertEquals(PickerFooterService.SaveResult.INVALID_URL,
                service.save("Logo", "javascript:alert(1)", "", null, false));
        assertEquals(PickerFooterService.SaveResult.INVALID_LINK,
                service.save("Logo", "https://example.org",
                        PickerFooterService.encode(List.of(new PickerFooterLink("Site", "notaurl"))),
                        null, false));
        assertEquals(PickerFooterService.SaveResult.INVALID_LABEL,
                service.save("   ", "https://example.org", "", null, false));
        verify(repository, never()).replaceLinks(any());
    }

    @Test
    void save_rejectsMoreThanThirtyLinks() {
        List<PickerFooterLink> links = new ArrayList<>();
        for (int i = 0; i < PickerFooterService.MAX_LINKS + 1; i++) {
            links.add(new PickerFooterLink("Lien " + i, "https://example.org/" + i));
        }

        assertEquals(PickerFooterService.SaveResult.TOO_MANY_LINKS,
                service.save("Logo", "https://example.org", PickerFooterService.encode(links), null, false));
        verify(repository, never()).replaceLinks(any());
    }

    @Test
    void save_storesAPngAndClearsOnRequest() {
        assertEquals(PickerFooterService.SaveResult.SAVED,
                service.save("Logo", "https://example.org", "", PNG, false));
        verify(repository).replaceImage(eq("Logo"), eq("https://example.org"), eq(PNG), eq("image/png"), anyLong());

        assertEquals(PickerFooterService.SaveResult.INVALID_IMAGE,
                service.save("Logo", "https://example.org", "", new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, false));

        assertEquals(PickerFooterService.SaveResult.SAVED,
                service.save("Logo", "https://example.org", "", null, true));
        verify(repository).clearImage("Logo", "https://example.org");
        assertFalse(service.load().customImage());
    }

    @Test
    void encode_roundTripsATitleWithSpaces() {
        List<PickerFooterLink> links = PickerFooterService.decode(
                PickerFooterService.encode(List.of(new PickerFooterLink("Info gouv", "https://www.info.gouv.fr"))));

        assertEquals("Info gouv", links.get(0).label());
        assertEquals("https://www.info.gouv.fr", links.get(0).url());

        List<PickerFooterLink> four = List.of(
                new PickerFooterLink("legifrance.gouv.fr", "https://www.legifrance.gouv.fr"),
                new PickerFooterLink("info.gouv.fr", "https://www.info.gouv.fr"),
                new PickerFooterLink("service-public.fr", "https://www.service-public.fr"),
                new PickerFooterLink("data.gouv.fr", "https://www.data.gouv.fr"));
        assertEquals(four, PickerFooterService.decode(PickerFooterService.encode(four)));
        assertEquals(four.subList(0, 3), PickerFooterService.decode(
                "legifrance.gouv.fr\thttps%3A%2F%2Fwww.legifrance.gouv.fr\n"
                        + "info.gouv.fr\thttps%3A%2F%2Fwww.info.gouv.fr\r\n"
                        + "service-public.fr\thttps%3A%2F%2Fwww.service-public.fr"));
        assertEquals(PickerFooterService.DEFAULT_MARK_PATH, PickerFooterService.markPath(false, 0));
        assertTrue(PickerFooterService.markPath(true, 4L).contains("picker-footer/mark?v=4"));
    }
}
