package auction.validators;

import auction.controllers.AuctionController;
import auction.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }
    Logger logger = LoggerFactory.getLogger(AuctionController.class);
    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "field.required", "Name is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "surname", "field.required", "Surname is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "login", "field.required", "Login is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required", "Password is required");
        if (user.getLogin() != null && user.getLogin().length() < 5) {
            errors.rejectValue("login", "field.min.length", "Login must be at least 5 characters long");
        }

        if (user.getPassword() != null && user.getPassword().length() < 5) {
            errors.rejectValue("password", "field.min.length", "Password must be at least 5 characters long");
        }
    }
}