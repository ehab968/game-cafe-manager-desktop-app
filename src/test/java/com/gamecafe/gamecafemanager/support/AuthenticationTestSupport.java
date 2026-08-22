package com.gamecafe.gamecafemanager.support;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.data.repository.SQLiteUserRepository;
import com.gamecafe.gamecafemanager.data.security.Pbkdf2PasswordHasher;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import java.util.Arrays;

public final class AuthenticationTestSupport {

    private static final String TEST_PASSWORD = "test-password";

    private AuthenticationTestSupport() {
    }

    public static AuthorizationService authenticatedAdmin(Database database) {
        return authenticated(database, Role.ADMIN);
    }

    public static AuthorizationService authenticated(Database database, Role role) {
        UserRepository userRepository = new SQLiteUserRepository(database);
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        String username = "test-" + role.name().toLowerCase();
        char[] password = TEST_PASSWORD.toCharArray();
        try {
            userRepository.create(
                    new User(null, username, role, true),
                    passwordHasher.hash(password));
            AuthenticationService authenticationService =
                    new AuthenticationService(userRepository, passwordHasher);
            authenticationService.authenticate(username, password);
            return new AuthorizationService(authenticationService);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
