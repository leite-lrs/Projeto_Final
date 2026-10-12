package com.flashsale.orderprocessor;

import java.util.Optional;

final class InMemoryOrderPersistenceRepository extends OrderPersistenceRepository {
    @Override
    public OrderRecord save(OrderRecord orderRecord) {
        return orderRecord;
    }

    @Override
    public Optional<OrderRecord> findByPedidoId(String pedidoId) {
        return Optional.empty();
    }
}
