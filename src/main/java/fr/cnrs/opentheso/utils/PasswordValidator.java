package fr.cnrs.opentheso.utils;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator<String> {

    private static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {

        // Récupérer le bean Spring manuellement
        WebApplicationContext springContext = FacesContextUtils.getWebApplicationContext(context);
        LanguageBean languageBean = springContext.getBean(LanguageBean.class);

        if (value == null || value.isEmpty()) {
            throw new ValidatorException(
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            languageBean.getMsg("password.empty"),
                            languageBean.getMsg("password.empty")));
        }

        if (!value.matches(REGEX)) {
            throw new ValidatorException(
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            languageBean.getMsg("password.invalid"),
                            languageBean.getMsg("password.invalid")));
        }
    }
}
