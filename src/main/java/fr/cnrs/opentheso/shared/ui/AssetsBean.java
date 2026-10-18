package fr.cnrs.opentheso.shared.ui;

import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Version unique des assets V2 (cache-bust). À incrémenter après un changement CSS/JS.
 */
@Component("assets")
public class AssetsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String VERSION = "cv-206";

    public String getV() {
        return VERSION;
    }
}
