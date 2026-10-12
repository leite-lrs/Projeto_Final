package com.flashsale.orderprocessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class OrderPersistenceRepository {
    private static final Logger LOG = Logger.getLogger(OrderPersistenceRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    public OrderRecord save(OrderRecord orderRecord) {
        if (orderRecord == null) {
            return null;
        }
        if (entityManager == null) {
            return orderRecord;
        }
        try {
            entityManager.merge(OrderEntity.from(orderRecord));
            return orderRecord;
        } catch (Exception exception) {
            LOG.warnv("Falha ao persistir pedido {0} em PostgreSQL: {1}", orderRecord.pedidoId(), exception.getMessage());
            return orderRecord;
        }
    }

    public Optional<OrderRecord> findByPedidoId(String pedidoId) {
        if (pedidoId == null || pedidoId.isBlank() || entityManager == null) {
            return Optional.empty();
        }
        try {
            OrderEntity entity = entityManager.find(OrderEntity.class, pedidoId);
            return Optional.ofNullable(entity).map(OrderEntity::toRecord);
        } catch (Exception exception) {
            LOG.warnv("Falha ao consultar pedido {0} em PostgreSQL: {1}", pedidoId, exception.getMessage());
            return Optional.empty();
        }
    }
}
