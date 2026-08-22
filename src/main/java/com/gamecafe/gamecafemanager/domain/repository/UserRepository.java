package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.PasswordHash;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User create(User user, PasswordHash passwordHash);

    List<User> findAll();

    Optional<User> findById(long id);

    Optional<UserAccount> findAccountByUsername(String username);

    boolean existsByUsername(String username);

    int count();

    int countEnabledAdmins();

    void updateRole(long id, Role role);

    void setEnabled(long id, boolean enabled);

    void updatePassword(long id, PasswordHash passwordHash);
}
