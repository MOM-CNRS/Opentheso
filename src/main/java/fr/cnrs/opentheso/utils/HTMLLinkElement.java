package fr.cnrs.opentheso.utils;

import lombok.Data;

@Data
public class HTMLLinkElement {

    private String linkElement;
    private String linkAddress;

    public void setLinkAddress(String linkElement) {
        this.linkElement = linkElement.replaceAll("'", "");
        this.linkElement = this.linkElement.replaceAll("\"", "");
    }

    @Override
    public String toString() {
        return "Link Address : " + this.linkAddress + ". Link Element : " + this.linkElement;
    }
}
