package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.shared.model.PickerFooterImage;
import fr.cnrs.opentheso.shared.model.PickerFooterLink;
import fr.cnrs.opentheso.shared.model.PickerFooterSettings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PickerFooterRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<PickerFooterSettings> findSettings() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT brand_label, brand_url,
                               CASE WHEN image IS NULL THEN false ELSE true END,
                               image_version
                        FROM picker_footer
                        WHERE id = 1
                        """)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new PickerFooterSettings(
                row[0] == null ? "" : String.valueOf(row[0]),
                row[1] == null ? "" : String.valueOf(row[1]),
                asBoolean(row[2]),
                asLong(row[3]),
                List.of()
        ));
    }

    @SuppressWarnings("unchecked")
    public List<PickerFooterLink> findLinks() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT label, url
                        FROM picker_footer_link
                        ORDER BY position
                        """)
                .getResultList();
        List<PickerFooterLink> links = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null) {
                continue;
            }
            links.add(new PickerFooterLink(
                    row[0] == null ? "" : String.valueOf(row[0]),
                    row[1] == null ? "" : String.valueOf(row[1])
            ));
        }
        return links;
    }

    @SuppressWarnings("unchecked")
    public Optional<PickerFooterImage> findImage() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT image, image_type, image_version
                        FROM picker_footer
                        WHERE id = 1 AND image IS NOT NULL
                        """)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        byte[] bytes = asBytes(row[0]);
        if (bytes.length == 0) {
            return Optional.empty();
        }
        return Optional.of(new PickerFooterImage(
                bytes,
                row[1] == null ? "" : String.valueOf(row[1]),
                asLong(row[2])
        ));
    }

    public void updateBrand(String label, String url) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_footer (id, brand_label, brand_url)
                        VALUES (1, :label, :url)
                        ON CONFLICT (id) DO UPDATE SET
                            brand_label = EXCLUDED.brand_label,
                            brand_url = EXCLUDED.brand_url
                        """)
                .setParameter("label", label)
                .setParameter("url", url)
                .executeUpdate();
    }

    public void replaceImage(String label, String url, byte[] image, String imageType, long imageVersion) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_footer (id, brand_label, brand_url, image, image_type, image_version)
                        VALUES (1, :label, :url, :image, :imageType, :imageVersion)
                        ON CONFLICT (id) DO UPDATE SET
                            brand_label = EXCLUDED.brand_label,
                            brand_url = EXCLUDED.brand_url,
                            image = EXCLUDED.image,
                            image_type = EXCLUDED.image_type,
                            image_version = EXCLUDED.image_version
                        """)
                .setParameter("label", label)
                .setParameter("url", url)
                .setParameter("image", image)
                .setParameter("imageType", imageType)
                .setParameter("imageVersion", imageVersion)
                .executeUpdate();
    }

    public void clearImage(String label, String url) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_footer (id, brand_label, brand_url)
                        VALUES (1, :label, :url)
                        ON CONFLICT (id) DO UPDATE SET
                            brand_label = EXCLUDED.brand_label,
                            brand_url = EXCLUDED.brand_url,
                            image = NULL,
                            image_type = NULL
                        """)
                .setParameter("label", label)
                .setParameter("url", url)
                .executeUpdate();
    }

    public void replaceLinks(List<PickerFooterLink> links) {
        entityManager.createNativeQuery("DELETE FROM picker_footer_link").executeUpdate();
        for (int i = 0; i < links.size(); i++) {
            PickerFooterLink link = links.get(i);
            entityManager.createNativeQuery("""
                            INSERT INTO picker_footer_link (position, label, url)
                            VALUES (:position, :label, :url)
                            """)
                    .setParameter("position", i)
                    .setParameter("label", link.label())
                    .setParameter("url", link.url())
                    .executeUpdate();
        }
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static byte[] asBytes(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        return new byte[0];
    }
}
