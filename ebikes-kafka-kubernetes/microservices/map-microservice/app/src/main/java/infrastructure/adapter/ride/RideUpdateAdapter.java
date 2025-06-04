package infrastructure.adapter.ride;
import static infrastructure.adapter.kafkatopic.Topics.RIDE_MAP_UPDATE;

import application.ports.RestMapServiceAPI;
import infrastructure.utils.KafkaProperties;
import infrastructure.utils.MetricsManager;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdateAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdateAdapter.class);
    private final RestMapServiceAPI mapService;
    private final MetricsManager metricsManager;
    private ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final KafkaProperties kafkaProperties;

    public RideUpdateAdapter(RestMapServiceAPI mapService, KafkaProperties kafkaProperties) {
        this.mapService = mapService;
        this.metricsManager = MetricsManager.getInstance();
        this.kafkaProperties = kafkaProperties;
    }

    public void init() {
        startKafkaConsumer();
        logger.info("RideUpdateAdapter initialized and Kafka consumer started");
    }

    private void startKafkaConsumer() {
        consumerExecutor = Executors.newSingleThreadExecutor();
        running.set(true);
        consumerExecutor.submit(this::runKafkaConsumer);
    }

    private void runKafkaConsumer() {
        KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(kafkaProperties.getConsumerProperties());

        try (consumer) {
            // Subscribe to the map-ride-update topic
            consumer.subscribe(List.of(RIDE_MAP_UPDATE.getTopicName()));
            logger.info("Subscribed to Kafka topic: {}", RIDE_MAP_UPDATE.getTopicName());

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            JsonObject rideUpdate = new JsonObject(record.value());
                            logger.info("Received ride update from Kafka: {}", rideUpdate);

                            // Process the ride update
                            processRideUpdate(rideUpdate);
                        } catch (Exception e) {
                            logger.error("Invalid ride update data from Kafka: {}", e.getMessage());
                        }
                    }
                    consumer.commitAsync(
                            (offsets, exception) -> {
                                if (exception != null) {
                                    logger.error("Failed to commit offsets: {}", exception.getMessage());
                                }
                            });
                } catch (Exception e) {
                    logger.error("Error during Kafka polling: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error setting up Kafka consumer: {}", e.getMessage());
        }
    }

    private void processRideUpdate(JsonObject rideUpdate) {
        String action = rideUpdate.getString("action");
        String username = rideUpdate.getString("username");
        String bikeName = rideUpdate.getString("bikeName");

        if (action == null || username == null || bikeName == null) {
            logger.error("Incomplete ride update data: {}", rideUpdate);
            return;
        }

        switch (action) {
            case "start":
                notifyStartRide(username, bikeName);
                break;
            case "stop":
                notifyStopRide(username, bikeName);
                break;
            default:
                logger.error("Unknown action in ride update: {}", action);
        }
    }

    private void notifyStartRide(String username, String bikeName) {
        metricsManager.incrementMethodCounter("notifyStartRide");
        var timer = metricsManager.startTimer();

        logger.info("Processing start ride notification for user: {} and bike: {}", username, bikeName);

        mapService
                .notifyStartRide(username, bikeName)
                .thenAccept(v -> {
                    logger.info("Start ride notification processed successfully");
                    metricsManager.recordTimer(timer, "notifyStartRide");
                })
                .exceptionally(ex -> {
                    logger.error("Error processing start ride notification: {}", ex.getMessage());
                    metricsManager.recordError(timer, "notifyStartRide", ex);
                    return null;
                });
    }

    private void notifyStopRide(String username, String bikeName) {
        metricsManager.incrementMethodCounter("notifyStopRide");
        var timer = metricsManager.startTimer();

        logger.info("Processing stop ride notification for user: {} and bike: {}", username, bikeName);

        mapService
                .notifyStopRide(username, bikeName)
                .thenAccept(v -> {
                    logger.info("Stop ride notification processed successfully");
                    metricsManager.recordTimer(timer, "notifyStopRide");
                })
                .exceptionally(ex -> {
                    logger.error("Error processing stop ride notification: {}", ex.getMessage());
                    metricsManager.recordError(timer, "notifyStopRide", ex);
                    return null;
                });
    }

    public void stop() {
        running.set(false);
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
        logger.info("RideUpdateAdapter Kafka consumer executor shut down.");
    }
}