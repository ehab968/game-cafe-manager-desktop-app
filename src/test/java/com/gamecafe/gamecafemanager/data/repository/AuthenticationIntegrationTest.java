package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.data.security.Pbkdf2PasswordHasher;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.InvalidCredentialsException;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.model.UserAccount;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import com.gamecafe.gamecafemanager.domain.usecase.auth.InitializeAdminUseCase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthenticationIntegrationTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "secure-password";

    @TempDir
    Path temporaryDirectory;

    private SQLiteDatabase database;
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(temporaryDirectory.resolve("authentication.db"));
        database.initialize();
        userRepository = new SQLiteUserRepository(database);
        passwordHasher = new Pbkdf2PasswordHasher();
        authenticationService = new AuthenticationService(userRepository, passwordHasher);
    }

    @Test
    void initializesFirstAdminOnceAndStoresOnlyPasswordVerifierMaterial() throws SQLException {
        User admin = initializeAdmin();

        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isEnabled());
        UserAccount account = userRepository.findAccountByUsername(ADMIN_USERNAME)
                .orElseThrow(AssertionError::new);
        assertEquals(Pbkdf2PasswordHasher.ALGORITHM,
                account.getPasswordHash().getAlgorithm());
        assertTrue(account.getPasswordHash().getIterations() >= 100_000);
        assertFalse(Arrays.equals(
                ADMIN_PASSWORD.getBytes(StandardCharsets.UTF_8),
                account.getPasswordHash().getHash()));

        Set<String> columns = userTableColumns();
        assertTrue(columns.contains("password_hash"));
        assertTrue(columns.contains("password_salt"));
        assertFalse(columns.contains("password"));

        char[] password = ADMIN_PASSWORD.toCharArray();
        try {
            assertThrows(IllegalStateException.class, () ->
                    new InitializeAdminUseCase(
                            userRepository, passwordHasher, new UserValidator())
                            .execute("second-admin", password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void authenticatesValidCredentialsAndRejectsWrongUnknownOrDisabledAccounts() {
        User admin = initializeAdmin();

        char[] correctPassword = ADMIN_PASSWORD.toCharArray();
        try {
            assertEquals(admin.getId(), authenticationService.authenticate(
                    "  ADMIN  ", correctPassword).getId());
        } finally {
            Arrays.fill(correctPassword, '\0');
        }
        authenticationService.logout();
        assertFalse(authenticationService.getCurrentUser().isPresent());

        assertInvalidCredentials(ADMIN_USERNAME, "wrong-password");
        assertInvalidCredentials("missing-user", ADMIN_PASSWORD);

        userRepository.setEnabled(admin.getId(), false);
        assertInvalidCredentials(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private User initializeAdmin() {
        char[] password = ADMIN_PASSWORD.toCharArray();
        try {
            return new InitializeAdminUseCase(
                    userRepository, passwordHasher, new UserValidator())
                    .execute(ADMIN_USERNAME, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void assertInvalidCredentials(String username, String suppliedPassword) {
        char[] password = suppliedPassword.toCharArray();
        try {
            assertThrows(InvalidCredentialsException.class, () ->
                    authenticationService.authenticate(username, password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Set<String> userTableColumns() throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(users)")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }
}
