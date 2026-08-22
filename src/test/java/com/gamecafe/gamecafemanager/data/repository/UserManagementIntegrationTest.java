package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.security.Pbkdf2PasswordHasher;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.InvalidCredentialsException;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import com.gamecafe.gamecafemanager.domain.usecase.auth.InitializeAdminUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.CreateUserUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.ResetUserPasswordUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.SetUserEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.UpdateUserRoleUseCase;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserManagementIntegrationTest {

    private static final String ADMIN_PASSWORD = "admin-password";

    @TempDir
    Path temporaryDirectory;

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private AuthenticationService authenticationService;
    private AuthorizationService authorization;
    private User admin;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("user-management.db"));
        database.initialize();
        userRepository = new SQLiteUserRepository(database);
        passwordHasher = new Pbkdf2PasswordHasher();
        authenticationService = new AuthenticationService(userRepository, passwordHasher);
        authorization = new AuthorizationService(authenticationService);
        char[] password = ADMIN_PASSWORD.toCharArray();
        try {
            admin = new InitializeAdminUseCase(
                    userRepository, passwordHasher, new UserValidator())
                    .execute("admin", password);
            authenticationService.authenticate("admin", password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void adminCreatesChangesResetsAndDisablesUser() {
        User cashier = createUser("  cashier-one  ", Role.CASHIER, "old-password");
        assertEquals("cashier-one", cashier.getUsername());

        new UpdateUserRoleUseCase(userRepository, authorization)
                .execute(cashier.getId(), Role.ADMIN);
        resetPassword(cashier.getId(), "new-password");
        new SetUserEnabledUseCase(userRepository, authorization)
                .execute(cashier.getId(), false);

        User stored = userRepository.findById(cashier.getId()).orElseThrow(AssertionError::new);
        assertEquals(Role.ADMIN, stored.getRole());
        assertFalse(stored.isEnabled());
        assertInvalidCredentials("cashier-one", "old-password");
        assertInvalidCredentials("cashier-one", "new-password");

        new SetUserEnabledUseCase(userRepository, authorization)
                .execute(cashier.getId(), true);
        authenticationService.logout();
        char[] newPassword = "new-password".toCharArray();
        try {
            assertEquals(cashier.getId(), authenticationService.authenticate(
                    "cashier-one", newPassword).getId());
        } finally {
            Arrays.fill(newPassword, '\0');
        }
    }

    @Test
    void preventsAdminFromRemovingOwnLastAdminAccess() {
        assertThrows(ValidationException.class, () ->
                new UpdateUserRoleUseCase(userRepository, authorization)
                        .execute(admin.getId(), Role.CASHIER));
        assertThrows(ValidationException.class, () ->
                new SetUserEnabledUseCase(userRepository, authorization)
                        .execute(admin.getId(), false));
    }

    private User createUser(String username, Role role, String suppliedPassword) {
        char[] password = suppliedPassword.toCharArray();
        try {
            return new CreateUserUseCase(
                    userRepository,
                    passwordHasher,
                    new UserValidator(),
                    authorization).execute(username, role, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void resetPassword(long userId, String suppliedPassword) {
        char[] password = suppliedPassword.toCharArray();
        try {
            new ResetUserPasswordUseCase(
                    userRepository,
                    passwordHasher,
                    new UserValidator(),
                    authorization).execute(userId, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void assertInvalidCredentials(String username, String suppliedPassword) {
        authenticationService.logout();
        char[] password = suppliedPassword.toCharArray();
        try {
            assertThrows(InvalidCredentialsException.class, () ->
                    authenticationService.authenticate(username, password));
        } finally {
            Arrays.fill(password, '\0');
        }
        char[] adminPassword = ADMIN_PASSWORD.toCharArray();
        try {
            authenticationService.authenticate("admin", adminPassword);
        } finally {
            Arrays.fill(adminPassword, '\0');
        }
    }
}
