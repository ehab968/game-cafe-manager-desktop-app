package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.exception.InvalidCredentialsException;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.model.UserAccount;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;

/**
 * Maintains the authenticated user for the current desktop process.
 */
public final class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private User currentUser;

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public synchronized User authenticate(String username, char[] password) {
        String normalizedUsername = username == null ? "" : username.trim();
        UserAccount account = userRepository.findAccountByUsername(normalizedUsername)
                .orElseThrow(InvalidCredentialsException::new);
        if (!account.getUser().isEnabled()
                || !passwordHasher.verify(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        currentUser = account.getUser();
        return currentUser;
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public synchronized Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }
}
