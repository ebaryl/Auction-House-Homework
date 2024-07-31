package auction.controllers;

import auction.model.User;
import auction.services.IAuthenticationService;
import auction.session.SessionConstants;
import auction.validators.UserValidator;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthenticationController {

    private final IAuthenticationService authenticationService;
    private final UserValidator userValidator;

    @Autowired
    HttpSession httpSession;

    public AuthenticationController(IAuthenticationService authenticationService, UserValidator userValidator) {
        this.authenticationService = authenticationService;
        this.userValidator = userValidator;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginInfo", this.authenticationService.getLoginInfo());
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String login, @RequestParam String password) {
        this.authenticationService.login(login, password);
        if(this.httpSession.getAttribute(SessionConstants.USER_KEY) != null) {
            return "redirect:/auctions";
        }
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    Logger logger = LoggerFactory.getLogger(AuctionController.class);

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user, BindingResult bindingResult, Model model) {
        userValidator.validate(user, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("has errors ");
            model.addAttribute("registererrors", bindingResult.getAllErrors().toString());
            return "redirect:/register";
        }
        boolean registered = this.authenticationService.register(user);
        if (registered) {
            return "redirect:/login";
        }
        return "redirect:/register";
    }

    @GetMapping("/logout")
    public String logout() {
        this.authenticationService.logout();
        return "redirect:/auctions";  // Redirect to auctions page after logout
    }
}