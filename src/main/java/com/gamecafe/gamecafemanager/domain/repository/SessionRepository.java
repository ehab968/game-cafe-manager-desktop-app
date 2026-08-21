package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.Session;
import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    Session create(Session session);

    Session finish(Session completedSession);

    Optional<Session> findById(long id);

    Optional<Session> findActiveByStationId(long stationId);

    List<Session> findActive();
}
