package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.Station;
import java.util.List;
import java.util.Optional;

/**
 * Persistence protocol for stations. Implementations belong to the data layer.
 */
public interface StationRepository {

    Station create(Station station);

    Station update(Station station);

    List<Station> findAll();

    Optional<Station> findById(long id);

    boolean existsByName(String name, Long excludedStationId);

    void setEnabled(long id, boolean enabled);
}
