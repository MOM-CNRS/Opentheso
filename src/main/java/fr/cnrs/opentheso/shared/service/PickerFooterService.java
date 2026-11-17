package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.model.PickerFooterImage;
import fr.cnrs.opentheso.shared.model.PickerFooterLink;
import fr.cnrs.opentheso.shared.model.PickerFooterSettings;
import fr.cnrs.opentheso.shared.repository.PickerFooterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PickerFooterService {

    public static final String DEFAULT_BRAND_LABEL = "Ministère de la Culture";
    public static final String DEFAULT_BRAND_URL = "https://www.culture.gouv.fr";
    public static final String DEFAULT_MARK_PATH = "/resources/img/minist%C3%A8re%20de%20la%20culture.png";
    public static final int LABEL_MAX = 120;
    public static final int URL_MAX = 500;
    public static final int MAX_LINKS = 30;
    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    public static final List<PickerFooterLink> DEFAULT_LINKS = List.of(
            new PickerFooterLink("legifrance.gouv.fr", "https://www.legifrance.gouv.fr"),
            new PickerFooterLink("info.gouv.fr", "https://www.info.gouv.fr"),
            new PickerFooterLink("service-public.fr", "https://www.service-public.fr"),
            new PickerFooterLink("data.gouv.fr", "https://www.data.gouv.fr")
    );

    public static final PickerFooterSettings DEFAULT = new PickerFooterSettings(
            DEFAULT_BRAND_LABEL, DEFAULT_BRAND_URL, false, 0L, DEFAULT_LINKS);

    private final PickerFooterRepository pickerFooterRepository;
    private volatile PickerFooterSettings cached;

    @Transactional(readOnly = true)
    public PickerFooterSettings load() {
        PickerFooterSettings current = cached;
        if (current != null) {
            return current;
        }
        current = read();
        cached = current;
        return current;
    }

    private PickerFooterSettings read() {
        return pickerFooterRepository.findSettings()
                .filter(settings -> StringUtils.isNotBlank(settings.brandLabel())
                        && StringUtils.isNotBlank(settings.brandUrl()))
                .map(settings -> new PickerFooterSettings(
                        settings.brandLabel(),
                        settings.brandUrl(),
                        settings.customImage(),
                        settings.imageVersion(),
                        List.copyOf(pickerFooterRepository.findLinks())
                ))
                .orElse(DEFAULT);
    }

    @Transactional(readOnly = true)
    public Optional<PickerFooterImage> image() {
        return pickerFooterRepository.findImage()
                .filter(image -> image.bytes() != null
                        && image.bytes().length > 0
                        && StringUtils.isNotBlank(image.contentType()));
    }

    @Transactional
    public SaveResult save(String brandLabel, String brandUrl, String linksPayload, byte[] image, boolean clearImage) {
        String cleanLabel = StringUtils.normalizeSpace(StringUtils.defaultString(brandLabel));
        if (cleanLabel.isEmpty() || cleanLabel.length() > LABEL_MAX) {
            return SaveResult.INVALID_LABEL;
        }
        String cleanUrl = StringUtils.trimToEmpty(brandUrl);
        if (!isHttpUrl(cleanUrl)) {
            return SaveResult.INVALID_URL;
        }
        List<PickerFooterLink> links = decode(linksPayload);
        if (links.size() > MAX_LINKS) {
            return SaveResult.TOO_MANY_LINKS;
        }
        for (PickerFooterLink link : links) {
            if (StringUtils.isBlank(link.label())
                    || link.label().length() > LABEL_MAX
                    || !isHttpUrl(link.url())) {
                return SaveResult.INVALID_LINK;
            }
        }
        List<PickerFooterLink> cleanLinks = links.stream()
                .map(link -> new PickerFooterLink(StringUtils.normalizeSpace(link.label()), link.url().trim()))
                .toList();
        boolean customImage;
        long imageVersion;
        if (image != null && image.length > 0) {
            String type = imageType(image);
            if (type == null) {
                return SaveResult.INVALID_IMAGE;
            }
            imageVersion = System.currentTimeMillis();
            pickerFooterRepository.replaceImage(cleanLabel, cleanUrl, image, type, imageVersion);
            customImage = true;
        } else if (clearImage) {
            pickerFooterRepository.clearImage(cleanLabel, cleanUrl);
            customImage = false;
            imageVersion = 0L;
        } else {
            PickerFooterSettings previous = load();
            pickerFooterRepository.updateBrand(cleanLabel, cleanUrl);
            customImage = previous.customImage();
            imageVersion = previous.imageVersion();
        }
        pickerFooterRepository.replaceLinks(cleanLinks);
        cached = new PickerFooterSettings(cleanLabel, cleanUrl, customImage, imageVersion, List.copyOf(cleanLinks));
        return SaveResult.SAVED;
    }

    public static String encode(List<PickerFooterLink> links) {
        if (links == null || links.isEmpty()) {
            return "";
        }
        StringBuilder payload = new StringBuilder();
        for (PickerFooterLink link : links) {
            if (!payload.isEmpty()) {
                payload.append('\u001e');
            }
            payload.append(urlEncode(link.label())).append('\t').append(urlEncode(link.url()));
        }
        return payload.toString();
    }

    public static List<PickerFooterLink> decode(String payload) {
        if (StringUtils.isBlank(payload)) {
            return List.of();
        }
        List<PickerFooterLink> links = new ArrayList<>();
        for (String line : payload.split("\u001e|\\r?\\n", -1)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            String label = urlDecode(parts[0]);
            String url = parts.length > 1 ? urlDecode(parts[1]) : "";
            if (label.isBlank() && url.isBlank()) {
                continue;
            }
            links.add(new PickerFooterLink(label, url));
        }
        return List.copyOf(links);
    }

    public static String markPath(boolean customImage, long imageVersion) {
        if (customImage) {
            return "/api/instance/picker-footer/mark?v=" + imageVersion;
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

    private static String urlEncode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    public enum SaveResult {
        SAVED,
        INVALID_LABEL,
        INVALID_URL,
        INVALID_LINK,
        TOO_MANY_LINKS,
        INVALID_IMAGE
    }
}
