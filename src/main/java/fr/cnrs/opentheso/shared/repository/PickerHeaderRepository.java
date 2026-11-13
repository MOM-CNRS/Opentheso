package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.shared.model.PickerHeaderImage;
import fr.cnrs.opentheso.shared.model.PickerHeaderSettings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PickerHeaderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<PickerHeaderSettings> findSettings() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT visible, link_label, link_url,
                               CASE WHEN image IS NULL THEN false ELSE true END,
                               image_version
                        FROM picker_header
                        WHERE id = 1
                        """)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new PickerHeaderSettings(
                asBoolean(row[0]),
                row[1] == null ? "" : String.valueOf(row[1]),
                row[2] == null ? "" : String.valueOf(row[2]),
                asBoolean(row[3]),
                asLong(row[4])
        ));
    }

    @SuppressWarnings("unchecked")
    public Optional<PickerHeaderImage> findImage() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT image, image_type, image_version
                        FROM picker_header
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
        return Optional.of(new PickerHeaderImage(
                bytes,
                row[1] == null ? "" : String.valueOf(row[1]),
                asLong(row[2])
        ));
    }

    public void updateText(boolean visible, String label, String url) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_header (id, visible, link_label, link_url)
                        VALUES (1, :visible, :label, :url)
                        ON CONFLICT (id) DO UPDATE SET
                            visible = EXCLUDED.visible,
                            link_label = EXCLUDED.link_label,
                            link_url = EXCLUDED.link_url
                        """)
                .setParameter("visible", visible)
                .setParameter("label", label)
                .setParameter("url", url)
                .executeUpdate();
    }

    public void replaceImage(boolean visible, String label, String url, byte[] image, String imageType, long imageVersion) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_header (id, visible, link_label, link_url, image, image_type, image_version)
                        VALUES (1, :visible, :label, :url, :image, :imageType, :imageVersion)
                        ON CONFLICT (id) DO UPDATE SET
                            visible = EXCLUDED.visible,
                            link_label = EXCLUDED.link_label,
                            link_url = EXCLUDED.link_url,
                            image = EXCLUDED.image,
                            image_type = EXCLUDED.image_type,
                            image_version = EXCLUDED.image_version
                        """)
                .setParameter("visible", visible)
                .setParameter("label", label)
                .setParameter("url", url)
                .setParameter("image", image)
                .setParameter("imageType", imageType)
                .setParameter("imageVersion", imageVersion)
                .executeUpdate();
    }

    public void clearImage(boolean visible, String label, String url) {
        entityManager.createNativeQuery("""
                        INSERT INTO picker_header (id, visible, link_label, link_url)
                        VALUES (1, :visible, :label, :url)
                        ON CONFLICT (id) DO UPDATE SET
                            visible = EXCLUDED.visible,
                            link_label = EXCLUDED.link_label,
                            link_url = EXCLUDED.link_url,
                            image = NULL,
                            image_type = NULL
                        """)
                .setParameter("visible", visible)
                .setParameter("label", label)
                .setParameter("url", url)
                .executeUpdate();
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
