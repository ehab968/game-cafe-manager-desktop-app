package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import java.util.List;

/**
 * Persistence protocol for session product purchases.
 */
public interface SessionProductRepository {

    /**
     * Atomically reserves stock, stores the purchase snapshots, and updates
     * session product/final totals.
     */
    SessionProduct addToActiveSession(long sessionId, long productId, int quantity);

    List<SessionProduct> findBySessionId(long sessionId);
}
