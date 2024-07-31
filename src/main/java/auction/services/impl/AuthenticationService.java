package auction.services.impl;

import auction.model.User;
import auction.services.IAuthenticationService;
import auction.session.SessionConstants;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import auction.dao.impl.spring.data.UserDAO;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final UserDAO userDAO;
    private final HttpSession httpSession;

    @Override
    public void login(String login, String password) {
        Optional<User> user = this.userDAO.findByLogin(login);
        if(user.isPresent() &&
                DigestUtils.md5DigestAsHex(password.getBytes()).equals(user.get().getPassword())) {
            httpSession.setAttribute(SessionConstants.USER_KEY, user.get());
            httpSession.setAttribute(SessionConstants.CART_KEY, new HashSet<>());
            return;
        }
        this.httpSession.setAttribute("loginInfo", "Invalid credentials");
    }

    @Override
    public void logout() {
        this.httpSession.removeAttribute(SessionConstants.USER_KEY);
        this.httpSession.removeAttribute(SessionConstants.CART_KEY);
    }

    @Override
    public String getLoginInfo() {
        String temp = (String) this.httpSession.getAttribute("loginInfo");
        this.httpSession.removeAttribute("loginInfo");
        return temp;
    }

    @Override
    public boolean register(User user) {
        if (userDAO.findByLogin(user.getLogin()).isPresent()) {
            this.httpSession.setAttribute("loginInfo", "Login already exists");
            return false;
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        userDAO.save(user);
        return true;
    }
}