package fr.cnrs.opentheso.shared.web;

import fr.cnrs.opentheso.shared.model.PickerHeaderImage;
import fr.cnrs.opentheso.shared.service.PickerHeaderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickerHeaderImageControllerTest {

    @Mock
    private PickerHeaderService pickerHeaderService;

    @Test
    void mark_returnsTheStoredImage() {
        byte[] png = new byte[] {1, 2, 3};
        when(pickerHeaderService.image()).thenReturn(Optional.of(new PickerHeaderImage(png, "image/png", 4L)));

        ResponseEntity<byte[]> response = new PickerHeaderImageController(pickerHeaderService).mark();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertArrayEquals(png, response.getBody());
    }

    @Test
    void mark_isNotFoundWithoutACustomImage() {
        when(pickerHeaderService.image()).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = new PickerHeaderImageController(pickerHeaderService).mark();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
