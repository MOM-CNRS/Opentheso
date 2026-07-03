package fr.cnrs.opentheso.v2.concept.support;

import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public final class ConceptGpsMapRenderer {

    private ConceptGpsMapRenderer() {
    }

    public static String renderMapScript(String mapElementId, List<ConceptGpsPoint> gpsPoints) {
        if (CollectionUtils.isEmpty(gpsPoints)) {
            return "";
        }
        GpsDisplayMode mode = resolveMode(gpsPoints);
        String latitudeCentre = average(gpsPoints, true);
        String longitudeCentre = average(gpsPoints, false);

        StringBuilder script = new StringBuilder();
        script.append("(function(){");
        script.append("var mapElement=document.getElementById('").append(mapElementId).append("');");
        script.append("if(!mapElement||typeof L==='undefined'){return;}");
        script.append("if(mapElement._leaflet_id){mapElement._leaflet_id=null; mapElement.innerHTML='';}");
        script.append("var map=L.map('").append(mapElementId).append("').setView([")
                .append(latitudeCentre).append(", ").append(longitudeCentre).append("], ")
                .append(calculerZoom(gpsPoints)).append(");");
        script.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{")
                .append("maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);");
        script.append("var latlngs=[");
        for (ConceptGpsPoint gps : gpsPoints) {
            script.append("[").append(gps.latitude()).append(",").append(gps.longitude()).append("],");
        }
        script.deleteCharAt(script.length() - 1);
        script.append("];");
        script.append("L.circleMarker(latlngs[0],{radius:5,color:'#0E53FF',fillColor:'#0E53FF',fillOpacity:1}).addTo(map);");
        if (mode != GpsDisplayMode.POINT) {
            if (mode == GpsDisplayMode.POLYGON) {
                script.append("var shape=L.polygon(latlngs,{color:'blue',fillColor:'#0E53FF',fillOpacity:0.5}).addTo(map);");
            } else {
                script.append("var shape=L.polyline(latlngs,{color:'blue',fillColor:'#0E53FF',fillOpacity:0.5}).addTo(map);");
            }
            script.append("map.fitBounds(shape.getBounds());");
        }
        script.append("})();");
        return script.toString();
    }

    private static GpsDisplayMode resolveMode(List<ConceptGpsPoint> gpsPoints) {
        if (gpsPoints.size() == 1) {
            return GpsDisplayMode.POINT;
        }
        ConceptGpsPoint first = gpsPoints.get(0);
        ConceptGpsPoint last = gpsPoints.get(gpsPoints.size() - 1);
        if (first.latitude() == last.latitude() && first.longitude() == last.longitude()) {
            return GpsDisplayMode.POLYGON;
        }
        return GpsDisplayMode.POLYLINE;
    }

    private static String average(List<ConceptGpsPoint> gpsPoints, boolean latitude) {
        double sum = gpsPoints.stream()
                .mapToDouble(point -> latitude ? point.latitude() : point.longitude())
                .sum();
        return Double.toString(sum / gpsPoints.size());
    }

    private static int calculerZoom(List<ConceptGpsPoint> gpsPoints) {
        final int zoomInitial = 13;
        if (gpsPoints == null || gpsPoints.size() < 2) {
            return zoomInitial;
        }
        final double facteurZoom = 0.12;
        double distanceTotale = 0;
        for (int i = 0; i < gpsPoints.size() - 1; i++) {
            distanceTotale += distanceEntrePoints(gpsPoints.get(i), gpsPoints.get(i + 1));
        }
        int niveauZoom = (int) (zoomInitial - facteurZoom * Math.log(distanceTotale));
        return Math.max(1, Math.min(18, niveauZoom));
    }

    private static double distanceEntrePoints(ConceptGpsPoint point1, ConceptGpsPoint point2) {
        double lat1 = Math.toRadians(point1.latitude());
        double lon1 = Math.toRadians(point1.longitude());
        double lat2 = Math.toRadians(point2.latitude());
        double lon2 = Math.toRadians(point2.longitude());
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }

    private enum GpsDisplayMode {
        POINT,
        POLYGON,
        POLYLINE
    }
}
