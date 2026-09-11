package mx.tecmilenio.testing;

public class UsernamePolicy {

    public static boolean isValid(String username) {
        if (username == null) {
            return false;
        }

        return username.length() >= 3
                && username.length() <= 12
                && username.matches("[a-zA-Z0-9_]+ ".trim());
    }
}
