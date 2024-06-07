package kz.medet;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
}
