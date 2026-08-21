package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StationManagementIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    private CreateStationUseCase createStation;
    private UpdateStationUseCase updateStation;
    private GetStationsUseCase getStations;
    private SetStationEnabledUseCase setStationEnabled;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(temporaryDirectory.resolve("stations.db"));
        database.initialize();
        StationRepository repository = new SQLiteStationRepository(database);
        StationValidator validator = new StationValidator();

        createStation = new CreateStationUseCase(repository, validator);
        updateStation = new UpdateStationUseCase(repository, validator);
        getStations = new GetStationsUseCase(repository);
        setStationEnabled = new SetStationEnabledUseCase(repository);
    }

    @Test
    void createsUpdatesListsAndDisablesStation() {
        Station created = createStation.execute(
                " Room 1 ",
                StationType.PLAYSTATION,
                new BigDecimal("125.5"));

        assertTrue(created.getId() > 0);
        assertEquals("Room 1", created.getName());
        assertEquals(new BigDecimal("125.50"), created.getHourlyRate());
        assertTrue(created.isEnabled());

        Station updated = updateStation.execute(
                created.getId(),
                "VIP Room",
                StationType.PLAYSTATION,
                new BigDecimal("200.00"));
        assertEquals("VIP Room", updated.getName());
        assertEquals(new BigDecimal("200.00"), updated.getHourlyRate());

        setStationEnabled.execute(created.getId(), false);
        List<Station> storedStations = getStations.execute();

        assertEquals(1, storedStations.size());
        assertEquals("VIP Room", storedStations.get(0).getName());
        assertFalse(storedStations.get(0).isEnabled());
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        createStation.execute(
                "Billiard 1",
                StationType.BILLIARD,
                new BigDecimal("80.00"));

        assertThrows(ValidationException.class, () -> createStation.execute(
                "billiard 1",
                StationType.BILLIARD,
                new BigDecimal("90.00")));
    }
}
