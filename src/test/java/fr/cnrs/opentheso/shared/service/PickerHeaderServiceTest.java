package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.model.PickerHeaderSettings;
import fr.cnrs.opentheso.shared.repository.PickerHeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickerHeaderServiceTest {

    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 0
    };

    @Mock
    private PickerHeaderRepository repository;

    private PickerHeaderService service;

    @BeforeEach
    void setUp() {
        service = new PickerHeaderService(repository);
    }

    @Test
    void load_readsTheStoredHeaderOnce() {
        when(repository.findSettings()).thenReturn(Optional.of(
                new PickerHeaderSettings(true, "culture.gouv.fr", "https://www.culture.gouv.fr", false, 3L)));

        assertEquals("culture.gouv.fr", service.load().label());
        assertEquals("culture.gouv.fr", service.load().label());

        verify(repository, times(1)).findSettings();
    }

    @Test
    void load_usesTheMinistryBarWhenNothingIsStored() {
        when(repository.findSettings()).thenReturn(Optional.empty());

        PickerHeaderSettings settings = service.load();

        assertTrue(settings.visible());
        assertEquals(PickerHeaderService.DEFAULT_LABEL, settings.label());
        assertEquals(PickerHeaderService.DEFAULT_URL, settings.url());
        assertFalse(settings.customImage());
        assertEquals(PickerHeaderService.DEFAULT_MARK_PATH, PickerHeaderService.markPath(false, 0));
    }

    @Test
    void save_updatesTextAndKeepsTheCurrentImage() {
        PickerHeaderService.SaveResult result = service.save(
                false, "  data.gouv.fr  ", "https://www.data.gouv.fr", null, false);

        assertEquals(PickerHeaderService.SaveResult.SAVED, result);
        verify(repository).updateText(false, "data.gouv.fr", "https://www.data.gouv.fr");
        verify(repository, never()).replaceImage(anyBoolean(), anyString(), anyString(), any(), anyString(), anyLong());
    }

    @Test
    void save_rejectsAMissingHostAndAScriptUrl() {
        assertEquals(PickerHeaderService.SaveResult.INVALID_URL,
                service.save(true, "Lien", "javascript:alert(1)", null, false));
        assertEquals(PickerHeaderService.SaveResult.INVALID_URL,
                service.save(true, "Lien", "https://", null, false));
        assertEquals(PickerHeaderService.SaveResult.INVALID_LABEL,
                service.save(true, "   ", "https://www.culture.gouv.fr", null, false));
        verify(repository, never()).updateText(anyBoolean(), anyString(), anyString());
    }

    @Test
    void save_storesAPngAndClearsOnRequest() {
        assertEquals(PickerHeaderService.SaveResult.SAVED,
                service.save(true, "culture.gouv.fr", "https://www.culture.gouv.fr", PNG, false));
        verify(repository).replaceImage(eq(true), eq("culture.gouv.fr"), eq("https://www.culture.gouv.fr"),
                eq(PNG), eq("image/png"), anyLong());

        assertEquals(PickerHeaderService.SaveResult.INVALID_IMAGE,
                service.save(true, "culture.gouv.fr", "https://www.culture.gouv.fr", new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, false));
        verify(repository, org.mockito.Mockito.times(1)).replaceImage(anyBoolean(), anyString(), anyString(), any(), anyString(), anyLong());

        assertEquals(PickerHeaderService.SaveResult.SAVED,
                service.save(true, "culture.gouv.fr", "https://www.culture.gouv.fr", null, true));
        verify(repository).clearImage(true, "culture.gouv.fr", "https://www.culture.gouv.fr");
    }
}
