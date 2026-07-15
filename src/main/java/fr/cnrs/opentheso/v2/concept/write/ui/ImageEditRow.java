package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ImageEditRow implements Serializable {

    private int id;
    private String uri;
    private String name;
    private String creator;
    private String copyright;

    public ImageEditRow() {
    }

    public ImageEditRow(int id, String uri, String name, String creator, String copyright) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.creator = creator;
        this.copyright = copyright;
    }
}
