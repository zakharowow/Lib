package validators;

import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("validators.LoginValidator")
public class LoginValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        ResourceBundle bundle = ResourceBundle.getBundle("nls.messages", FacesContext.getCurrentInstance().getViewRoot().getLocale());

        try {
            String newValue = value.toString().trim().toLowerCase();
            
            if (getTestArray().contains(newValue)) {
                throw new IllegalArgumentException(bundle.getString("name_error"));
            }

            if (newValue.substring(0, 1).matches("^[0-9]")) {
                throw new IllegalArgumentException(bundle.getString("namedigit_error"));
            }

            if (newValue.length() < 2) {
                throw new IllegalArgumentException(bundle.getString("login_error"));
            }

        } catch (IllegalArgumentException e) {
            FacesMessage message = new FacesMessage(e.getMessage());
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }
    private ArrayList<String> getTestArray(){
    ArrayList <String> list = new ArrayList<>();
    list.add("username");
    list.add("login");
    return list;
    }
}
