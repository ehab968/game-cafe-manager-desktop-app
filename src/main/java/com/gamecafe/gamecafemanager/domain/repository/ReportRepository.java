package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.CompletedSessionsReport;
import com.gamecafe.gamecafemanager.domain.model.ReportPeriod;

public interface ReportRepository {

    CompletedSessionsReport getCompletedSessionsReport(ReportPeriod period);
}
