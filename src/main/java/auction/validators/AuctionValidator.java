package auction.validators;

import auction.model.Auction;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

@Component
public class AuctionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Auction.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Auction auction = (Auction) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "title", "field.required", "Title is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "field.required", "Description is required");

        if (auction.getStartingPrice() == null || auction.getStartingPrice().doubleValue() <= 0) {
            errors.rejectValue("startingPrice", "field.invalid", "Starting price must be a positive number");
        }

        if (auction.getBuyNowPrice() == null || auction.getBuyNowPrice().doubleValue() <= 0) {
            errors.rejectValue("buyNowPrice", "field.invalid", "Buy Now price must be a positive number");
        }

        if (auction.getBuyNowPrice() != null && auction.getStartingPrice() != null &&
                auction.getBuyNowPrice().compareTo(auction.getStartingPrice()) <= 0) {
            errors.rejectValue("buyNowPrice", "field.invalid", "Buy Now price must be greater than Starting price");
        }

        if (auction.getEndTime() == null || auction.getEndTime().isBefore(LocalDateTime.now())) {
            errors.rejectValue("endTime", "field.invalid", "End time must be in the future");
        }

        if (auction.getCategory() == null || auction.getCategory().trim().isEmpty()) {
            errors.rejectValue("category", "field.required", "Category is required");
        }
    }
}