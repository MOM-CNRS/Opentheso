package fr.cnrs.opentheso.v2.shared.ui;

import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Version unique des assets V2 (cache-bust). À incrémenter après un changement CSS/JS.
 */
@Component("v2Assets")
public class V2AssetsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String VERSION = "cv-80";

    public String getV() {
        return VERSION;
    }
}
