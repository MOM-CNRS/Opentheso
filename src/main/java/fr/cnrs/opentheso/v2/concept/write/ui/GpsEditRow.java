package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GpsEditRow implements Serializable {

    private String latitude;
    private String longitude;

    public GpsEditRow() {
        this("", "");
    }

    public GpsEditRow(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
