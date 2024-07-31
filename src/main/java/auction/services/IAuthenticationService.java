package auction.services;

import auction.model.User;

public interface IAuthenticationService {
    void login(String login, String password);
    void logout();
    String getLoginInfo();
    boolean register(User user);

}