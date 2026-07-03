package fr.cnrs.opentheso.v2.concept.ui;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.ResponsiveOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Named("v2ConceptGalleriaBean")
@ViewScoped
public class ConceptGalleriaBean implements Serializable {

    private List<ResponsiveOption> responsiveOptions;
    private int activeIndex;

    @PostConstruct
    public void init() {
        responsiveOptions = new ArrayList<>();
        responsiveOptions.add(new ResponsiveOption("1024px", 5));
        responsiveOptions.add(new ResponsiveOption("768px", 3));
        responsiveOptions.add(new ResponsiveOption("560px", 1));
    }

    public void changeActiveIndex() {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
        activeIndex = Integer.parseInt(params.get("index"));
    }
}
