package fr.cnrs.opentheso.v2.shared.chart;

import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Chart.js configs for PrimeFaces 15 {@code p:chart} (legacy DonutChartModel removed).
 */
public final class ChartJsonSupport {

    private static final Gson GSON = new Gson();

    private ChartJsonSupport() {
    }

    public static String doughnut(List<? extends Number> values, List<String> labels, List<String> backgroundColors) {
        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("data", values);
        dataset.put("backgroundColor", backgroundColors);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("labels", labels);
        data.put("datasets", List.of(dataset));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "doughnut");
        root.put("data", data);
        return GSON.toJson(root);
    }
}
