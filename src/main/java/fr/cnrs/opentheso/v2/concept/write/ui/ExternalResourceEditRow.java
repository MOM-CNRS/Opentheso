package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExternalResourceEditRow implements Serializable {

    private String oldUri;
    private String uri;
    private String description;

    public ExternalResourceEditRow() {
    }

    public ExternalResourceEditRow(String uri, String description) {
        this.oldUri = uri;
        this.uri = uri;
        this.description = description;
    }
}
