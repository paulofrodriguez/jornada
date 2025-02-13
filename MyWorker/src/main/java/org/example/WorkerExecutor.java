package org.example;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WorkerExecutor {

    private static final String HOST = "localhost"; // Altere para o seu host do RabbitMQ
    private static final String USERNAME = "guest"; // Altere para o seu usuário
    private static final String PASSWORD = "guest"; // Altere para sua senha

    private static final String[] QUEUES = {"q_data", "q_events", "q_config", "q_control"};
    private static final String[] RESPONSE_QUEUES = {"q_data_response", "q_events_response"};

    private final ExecutorService executor = Executors.newFixedThreadPool(4); // 4 threads para as filas
    private final ExecutorService responseExecutor = Executors.newFixedThreadPool(2); // 2 threads para as filas de resposta

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private Connection connection;
    private Channel channel;

    public WorkerExecutor() {
        // Configuração do Circuit Breaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .minimumNumberOfCalls(10)
                .build();
        circuitBreaker = CircuitBreaker.of("rabbitmq", circuitBreakerConfig);

        // Configuração do Retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .build();
        retry = Retry.of("rabbitmq", retryConfig);

        // Tenta conectar ao RabbitMQ com Circuit Breaker e Retry
        connectToRabbitMQ();
    }

    private void connectToRabbitMQ() {
        try {
            // Usa o Circuit Breaker para proteger a conexão com o RabbitMQ
            CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
                try {
                    ConnectionFactory factory = new ConnectionFactory();
                    factory.setHost(HOST);
                    factory.setUsername(USERNAME);
                    factory.setPassword(PASSWORD);
                    connection = factory.newConnection();
                    channel = connection.createChannel();

                    // Declaração das filas
                    for (String queue : QUEUES) {
                        channel.queueDeclare(queue, false, false, false, null);
                    }
                    for (String queue : RESPONSE_QUEUES) {
                        channel.queueDeclare(queue, false, false, false, null);
                    }

                    System.out.println("Conectado ao RabbitMQ com sucesso!");
                } catch (IOException | TimeoutException e) {
                    throw new RuntimeException("Erro ao conectar ao RabbitMQ: " + e.getMessage());
                }
            }).run();
        } catch (Exception e) {
            System.err.println("Falha ao conectar ao RabbitMQ: " + e.getMessage());
            // Aqui você pode decidir o que fazer em caso de falha (ex: tentar novamente mais tarde)
        }
    }

    public void start() {
        // Consumir mensagens das filas
        for (String queue : QUEUES) {
            executor.submit(() -> {
                try {
                    consumeMessage(queue);
                } catch (IOException e) {
                    System.err.println("Erro ao consumir mensagens da fila " + queue + ": " + e.getMessage());
                }
            });
        }

        System.out.println("Programa principal em execução...");

        // Aguarda a interrupção do programa (Ctrl+C)
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println("Programa principal interrompido.");
        }

        System.out.println("Programa encerrado.");
    }

    private void consumeMessage(String queue) throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + queue + "': " + message);
            // Processar a mensagem...
        };

        channel.basicConsume(queue, true, deliverCallback, consumerTag -> { });
    }

    public void sendMessage(String queue, String message) {
        responseExecutor.submit(() -> {
            try {
                // Executa a operação dentro do Circuit Breaker e do Retry
                Retry.decorateRunnable(retry, () -> {
                    CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
                        try {
                            channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
                            System.out.println(" [x] Sent '" + queue + "': " + message);
                        } catch (IOException e) {
                            throw new RuntimeException("Erro ao enviar mensagem para " + queue + ": " + e.getMessage());
                        }
                    }).run();
                }).run();
            } catch (Exception e) {
                System.err.println("Falha ao enviar mensagem para " + queue + ": " + e.getMessage());
            }
        });
    }

    public void close() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        executor.shutdown();
        responseExecutor.shutdown();
    }

    public static void main(String[] args) {
        try {
            WorkerExecutor worker = new WorkerExecutor();
            worker.start();

            // Exemplo de envio de mensagem
            worker.sendMessage("q_data_response", "Mensagem de resposta para q_data");

            // Aguarda um pouco antes de encerrar (apenas para demonstração)
            Thread.sleep(5000);

            worker.close();
        } catch (Exception e) {
            System.err.println("Erro no programa principal: " + e.getMessage());
        }
    }
}