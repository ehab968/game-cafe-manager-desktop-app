package com.gamecafe.gamecafemanager.domain.usecase.invoice;

import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.InvoiceService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class GenerateInvoiceUseCase {

    private final SessionRepository sessionRepository;
    private final SessionProductRepository sessionProductRepository;
    private final InvoiceService invoiceService;
    private final AuthorizationService authorization;

    public GenerateInvoiceUseCase(
            SessionRepository sessionRepository,
            SessionProductRepository sessionProductRepository,
            InvoiceService invoiceService,
            AuthorizationService authorization) {
        this.sessionRepository = Objects.requireNonNull(
                sessionRepository, "sessionRepository");
        this.sessionProductRepository = Objects.requireNonNull(
                sessionProductRepository, "sessionProductRepository");
        this.invoiceService = Objects.requireNonNull(invoiceService, "invoiceService");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Invoice execute(long sessionId) {
        authorization.require(Permission.CHECKOUT);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        return invoiceService.generate(
                session,
                sessionProductRepository.findBySessionId(sessionId));
    }
}
