package com.flashsale.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public final class RabbitMqClient {
    private static final Logger LOG = Logger.getLogger(RabbitMqClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HOST = System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
    private static final String USER = System.getenv().getOrDefault("RABBITMQ_DEFAULT_USER", "guest");
    private static final String PASS = System.getenv().getOrDefault("RABBITMQ_DEFAULT_PASS", "guest");

    private RabbitMqClient() {}

    public static void publish(String queueName, Map<String, Object> payload) {
        try {
            publishMessage(queueName, payload);
        } catch (Exception exception) {
            logPublishFailure(queueName, exception);
        }
    }

    private static void publishMessage(String queueName, Map<String, Object> payload) throws Exception {
        try (Connection connection = connection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);
            byte[] body = OBJECT_MAPPER.writeValueAsBytes(payload);
            channel.basicPublish("", queueName, null, body);
            LOG.infov("Mensagem publicada na fila {0}", queueName);
        }
    }

    private static void logPublishFailure(String queueName, Exception exception) {
        LOG.warnv("Fila {0} indisponível, mantendo fallback local: {1}", queueName, exception.getMessage());
    }

    public static void consume(String queueName, Consumer<Map<String, Object>> handler) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection connection = connection()) {
                    if (connection == null) {
                        Thread.sleep(5000L);
                        continue;
                    }
                    Channel channel = connection.createChannel();
                    channel.queueDeclare(queueName, true, false, false, null);
                    DeliverCallback callback = (consumerTag, delivery) -> {
                        try {
                            Map<String, Object> payload = OBJECT_MAPPER.readValue(
                                new String(delivery.getBody(), StandardCharsets.UTF_8),
                                Map.class);
                            handler.accept(payload);
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception exception) {
                            LOG.warnv("Falha ao processar mensagem da fila {0}: {1}", queueName, exception.getMessage());
                            try {
                                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                            } catch (Exception ignored) {
                                LOG.debugv("Nack da fila {0} falhou", queueName);
                            }
                        }
                    };
                    channel.basicConsume(queueName, false, callback, consumerTag -> {});
                    while (connection.isOpen()) {
                        Thread.sleep(1000L);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception exception) {
                    LOG.warnv("Consumidor da fila {0} indisponível: {1}", queueName, exception.getMessage());
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static Connection connection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USER);
        factory.setPassword(PASS);
        return factory.newConnection();
    }
}
