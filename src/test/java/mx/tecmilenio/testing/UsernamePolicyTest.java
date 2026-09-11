package mx.tecmilenio.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernamePolicyTest {

    @Test
    void usernameWithThreeCharactersShouldBeValid() {
        boolean result = UsernamePolicy.isValid("ana");

        assertTrue(result);
    }

    @Test
    void usernameWithTwoCharactersShouldBeInvalid() {
        assertFalse(UsernamePolicy.isValid("an"));
    }

    @Test
    void nullUsernameShouldBeInvalid() {
        assertFalse(UsernamePolicy.isValid(null));
    }

    @Test
    void usernameWithSpacesShouldBeInvalid() {
        assertFalse(UsernamePolicy.isValid("ana maria"));
    }
}
