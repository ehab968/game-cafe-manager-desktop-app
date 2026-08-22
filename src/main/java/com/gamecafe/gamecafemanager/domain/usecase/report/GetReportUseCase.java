package com.gamecafe.gamecafemanager.domain.usecase.report;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.CompletedSessionsReport;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.ReportPeriod;
import com.gamecafe.gamecafemanager.domain.repository.ReportRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public final class GetReportUseCase {

    private final ReportRepository repository;
    private final AuthorizationService authorization;
    private final Clock clock;
    private final ZoneId zoneId;

    public GetReportUseCase(
            ReportRepository repository,
            AuthorizationService authorization,
            Clock clock,
            ZoneId zoneId) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    public CompletedSessionsReport executeToday() {
        authorization.require(Permission.VIEW_REPORTS);
        LocalDate today = LocalDate.now(clock.withZone(zoneId));
        return load(today, today);
    }

    public CompletedSessionsReport executeDay(LocalDate date) {
        authorization.require(Permission.VIEW_REPORTS);
        if (date == null) {
            throw ValidationException.forField("date", "Report date is required");
        }
        return load(date, date);
    }

    public CompletedSessionsReport execute(LocalDate startDate, LocalDate endDate) {
        authorization.require(Permission.VIEW_REPORTS);
        if (startDate == null) {
            throw ValidationException.forField("startDate", "Start date is required");
        }
        if (endDate == null) {
            throw ValidationException.forField("endDate", "End date is required");
        }
        if (endDate.isBefore(startDate)) {
            throw ValidationException.forField(
                    "endDate", "End date cannot be before start date");
        }
        return load(startDate, endDate);
    }

    private CompletedSessionsReport load(LocalDate startDate, LocalDate endDate) {
        try {
            return repository.getCompletedSessionsReport(
                    new ReportPeriod(startDate, endDate, zoneId));
        } catch (DateTimeException exception) {
            throw ValidationException.forField("endDate", "Report date range is invalid");
        }
    }
}
