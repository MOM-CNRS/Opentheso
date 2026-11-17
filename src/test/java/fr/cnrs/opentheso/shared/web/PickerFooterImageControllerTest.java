package fr.cnrs.opentheso.shared.web;

import fr.cnrs.opentheso.shared.model.PickerFooterImage;
import fr.cnrs.opentheso.shared.service.PickerFooterService;
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
class PickerFooterImageControllerTest {

    @Mock
    private PickerFooterService pickerFooterService;

    @Test
    void mark_returnsTheStoredImage() {
        byte[] png = new byte[] {1, 2, 3};
        when(pickerFooterService.image()).thenReturn(Optional.of(new PickerFooterImage(png, "image/png", 4L)));

        ResponseEntity<byte[]> response = new PickerFooterImageController(pickerFooterService).mark();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertArrayEquals(png, response.getBody());
    }

    @Test
    void mark_isNotFoundWithoutACustomImage() {
        when(pickerFooterService.image()).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = new PickerFooterImageController(pickerFooterService).mark();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
