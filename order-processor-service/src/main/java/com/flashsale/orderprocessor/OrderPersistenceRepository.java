package com.flashsale.orderprocessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class OrderPersistenceRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public OrderRecord save(OrderRecord orderRecord) {
        if (orderRecord == null) {
            return null;
        }
        requireEntityManager();
        entityManager.merge(OrderEntity.from(orderRecord));
        return orderRecord;
    }

    @Transactional
    public Optional<OrderRecord> findByPedidoId(String pedidoId) {
        if (isInvalidPedidoId(pedidoId)) {
            return Optional.empty();
        }
        requireEntityManager();
        return findOrder(pedidoId);
    }

    private boolean isInvalidPedidoId(String pedidoId) {
        return pedidoId == null || pedidoId.isBlank();
    }

    private void requireEntityManager() {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager não configurado");
        }
    }

    private Optional<OrderRecord> findOrder(String pedidoId) {
        OrderEntity entity = entityManager.find(OrderEntity.class, pedidoId);
        return Optional.ofNullable(entity).map(OrderEntity::toRecord);
    }
}
