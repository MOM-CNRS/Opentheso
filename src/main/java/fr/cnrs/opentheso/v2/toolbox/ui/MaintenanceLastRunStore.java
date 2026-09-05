package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component("v2MaintenanceLastRunStore")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MaintenanceLastRunStore implements Serializable {

    public static final String TOP_TERM = "topTerm";
    public static final String RESTRUCTURE = "restructure";
    public static final String COLLECTIONS = "collections";
    public static final String ROLES = "roles";
    public static final String ARK = "ark";
    public static final String SITEMAP = "sitemap";

    private final ConcurrentHashMap<String, Instant> lastRuns = new ConcurrentHashMap<>();
    private PendingSitemap pendingSitemap;

    public void mark(String thesaurusId, String action) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(action)) {
            return;
        }
        lastRuns.put(key(thesaurusId, action), V2Dates.nowInstant());
    }

    public Instant get(String thesaurusId, String action) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(action)) {
            return null;
        }
        return lastRuns.get(key(thesaurusId, action));
    }

    public void putPendingSitemap(String fileName, byte[] xml) {
        pendingSitemap = xml == null ? null : new PendingSitemap(fileName, xml);
    }

    public PendingSitemap consumePendingSitemap() {
        PendingSitemap pending = pendingSitemap;
        pendingSitemap = null;
        return pending;
    }

    private static String key(String thesaurusId, String action) {
        return thesaurusId + "|" + action;
    }

    public record PendingSitemap(String fileName, byte[] xml) implements Serializable {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PendingSitemap that)) {
                return false;
            }
            return Objects.equals(fileName, that.fileName) && Arrays.equals(xml, that.xml);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName, Arrays.hashCode(xml));
        }

        @Override
        public String toString() {
            return "PendingSitemap[fileName=" + fileName + ", bytes=" + (xml == null ? 0 : xml.length) + "]";
        }
    }
}
