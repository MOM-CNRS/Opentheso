package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.model.PickerHeaderImage;
import fr.cnrs.opentheso.shared.model.PickerHeaderSettings;
import fr.cnrs.opentheso.shared.repository.PickerHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PickerHeaderService {

    public static final String DEFAULT_LABEL = "culture.gouv.fr";
    public static final String DEFAULT_URL = "https://www.culture.gouv.fr";
    public static final String DEFAULT_MARK_PATH = "/resources/img/minist%C3%A8re%20de%20la%20culture.png";
    public static final int LABEL_MAX = 120;
    public static final int URL_MAX = 500;
    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    public static final PickerHeaderSettings DEFAULT = new PickerHeaderSettings(
            true, DEFAULT_LABEL, DEFAULT_URL, false, 0L);

    private final PickerHeaderRepository pickerHeaderRepository;
    private volatile PickerHeaderSettings cached;

    @Transactional(readOnly = true)
    public PickerHeaderSettings load() {
        PickerHeaderSettings current = cached;
        if (current != null) {
            return current;
        }
        current = read();
        cached = current;
        return current;
    }

    private PickerHeaderSettings read() {
        return pickerHeaderRepository.findSettings()
                .filter(settings -> StringUtils.isNotBlank(settings.label()) && StringUtils.isNotBlank(settings.url()))
                .orElse(DEFAULT);
    }

    @Transactional(readOnly = true)
    public Optional<PickerHeaderImage> image() {
        return pickerHeaderRepository.findImage()
                .filter(image -> image.bytes() != null
                        && image.bytes().length > 0
                        && StringUtils.isNotBlank(image.contentType()));
    }

    @Transactional
    public SaveResult save(boolean visible, String label, String url, byte[] image, boolean clearImage) {
        String cleanLabel = StringUtils.normalizeSpace(StringUtils.defaultString(label));
        if (cleanLabel.isEmpty() || cleanLabel.length() > LABEL_MAX) {
            return SaveResult.INVALID_LABEL;
        }
        String cleanUrl = StringUtils.trimToEmpty(url);
        if (!isHttpUrl(cleanUrl)) {
            return SaveResult.INVALID_URL;
        }
        if (image != null && image.length > 0) {
            String type = imageType(image);
            if (type == null) {
                return SaveResult.INVALID_IMAGE;
            }
            long version = System.currentTimeMillis();
            pickerHeaderRepository.replaceImage(visible, cleanLabel, cleanUrl, image, type, version);
            cached = new PickerHeaderSettings(visible, cleanLabel, cleanUrl, true, version);
            return SaveResult.SAVED;
        }
        if (clearImage) {
            pickerHeaderRepository.clearImage(visible, cleanLabel, cleanUrl);
            cached = new PickerHeaderSettings(visible, cleanLabel, cleanUrl, false, 0L);
            return SaveResult.SAVED;
        }
        PickerHeaderSettings previous = load();
        pickerHeaderRepository.updateText(visible, cleanLabel, cleanUrl);
        cached = new PickerHeaderSettings(visible, cleanLabel, cleanUrl, previous.customImage(), previous.imageVersion());
        return SaveResult.SAVED;
    }

    public static String markPath(boolean customImage, long imageVersion) {
        if (customImage) {
            return "/api/instance/picker-header/mark?v=" + imageVersion;
        }
        return DEFAULT_MARK_PATH;
    }

    static String imageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12 || bytes.length > MAX_IMAGE_BYTES) {
            return null;
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "image/gif";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean isHttpUrl(String value) {
        if (value.isEmpty() || value.length() > URL_MAX || value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && StringUtils.isNotBlank(uri.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public enum SaveResult {
        SAVED,
        INVALID_LABEL,
        INVALID_URL,
        INVALID_IMAGE
    }
}
