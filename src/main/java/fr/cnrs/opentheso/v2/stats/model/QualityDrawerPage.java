package fr.cnrs.opentheso.v2.stats.model;

import java.util.List;

public record QualityDrawerPage<T>(long total, List<T> items) {

    public static <T> QualityDrawerPage<T> empty() {
        return new QualityDrawerPage<>(0, List.of());
    }
}
