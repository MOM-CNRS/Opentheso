package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Télécharge et intègre les images externes dans le PDF (option "Ajouter les images").
 */
@Slf4j
final class ThesaurusPdfImageEmbedder {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String USER_AGENT = "Opentheso-PDF-Export";

    private ThesaurusPdfImageEmbedder() {
    }

    static void addImages(List<Paragraph> paragraphs, List<NodeImage> images, float horizontalOffset,
                          ThesaurusPdfSettings writePdfSettings) {
        if (CollectionUtils.isEmpty(images)) {
            return;
        }
        paragraphs.add(new Paragraph(Chunk.NEWLINE));
        for (NodeImage imageElement : images) {
            String uri = imageElement != null ? StringUtils.trimToNull(imageElement.getUri()) : null;
            if (uri == null) {
                paragraphs.add(new Paragraph("Image invalide : URI manquante"));
                continue;
            }
            try {
                Image image = loadImage(uri);
                float scaleFactor = writePdfSettings.resiseImage(image);
                if (scaleFactor <= 0) {
                    paragraphs.add(new Paragraph("Erreur de redimensionnement de l'image : " + uri));
                    continue;
                }
                image.scaleAbsolute(image.getWidth() / scaleFactor, image.getHeight() / scaleFactor);
                paragraphs.add(new Paragraph(new Chunk(image, horizontalOffset, 0, true)));
            } catch (Exception ex) {
                log.warn("Impossible d'intégrer l'image PDF depuis {}: {}", uri, ex.toString());
                paragraphs.add(new Paragraph("Erreur de téléchargement de l'image (image vide) : " + uri));
            }
        }
    }

    private static Image loadImage(String uri) throws Exception {
        URL imageUrl = URI.create(uri).toURL();
        URLConnection connection = imageUrl.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (connection instanceof HttpURLConnection http) {
            http.setInstanceFollowRedirects(true);
        }
        try (InputStream inputStream = connection.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                throw new IllegalStateException("Image vide");
            }
            return Image.getInstance(bytes);
        } finally {
            if (connection instanceof HttpURLConnection http) {
                http.disconnect();
            }
        }
    }
}
