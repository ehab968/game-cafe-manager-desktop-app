package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(char[] password);

    boolean verify(char[] password, PasswordHash expected);
}
