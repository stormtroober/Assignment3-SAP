package infrastructure.adapter.ride;

import static infrastructure.adapter.kafkatopic.Topics.RIDE_MAP_UPDATE;

import application.ports.BikeMapServiceAPI;
import domain.model.BikeType;
import infrastructure.utils.KafkaProperties;
import infrastructure.utils.MetricsManager;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdateAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideUpdateAdapter.class);
  private final BikeMapServiceAPI bikeMapService;
  private final MetricsManager metricsManager;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public RideUpdateAdapter(BikeMapServiceAPI bikeMapService) {
    this.bikeMapService = bikeMapService;
    this.metricsManager = MetricsManager.getInstance();
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
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

    try (consumer) {
      // Subscribe to both topics
      consumer.subscribe(List.of(RIDE_MAP_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", RIDE_MAP_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              if (record.topic().equals(RIDE_MAP_UPDATE.getTopicName())) {
                JsonObject rideUpdate = new JsonObject(record.value());
                logger.info("Received ride update from Kafka: {}", rideUpdate);
                processRideUpdate(rideUpdate);
              }
            } catch (Exception e) {
              logger.error("Invalid data from Kafka: {}", e.getMessage());
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
    String bikeTypeStr = rideUpdate.getString("bikeType");

    if (action == null || username == null || bikeName == null || bikeTypeStr == null) {
      logger.error("Incomplete ride update data: {}", rideUpdate);
      return;
    }

    BikeType bikeType;
    try {
      bikeType = BikeType.valueOf(bikeTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.error(
          "Invalid bike type: {}. Must be one of: {}",
          bikeTypeStr,
          Arrays.stream(BikeType.values()).map(Enum::name).collect(Collectors.joining(", ")));
      return;
    }

    switch (action) {
      case "start":
        notifyStartRide(username, bikeName, bikeType);
        break;
      case "stop":
        notifyStopRide(username, bikeName, bikeType);
        break;
      case "user_start":
        notifyStartRide(username, bikeName, bikeType);
        break;
      case "user_stop":
        notifyStopRide(username, bikeName, bikeType);
        break;
      default:
        logger.error("Unknown action in ride update: {}", action);
    }
  }

  private void notifyStartRide(String username, String bikeName, BikeType bikeType) {
    metricsManager.incrementMethodCounter("notifyStartRide");
    var timer = metricsManager.startTimer();

    logger.info("Processing start ride notification for user: {} and bike: {}", username, bikeName);

    bikeMapService
        .notifyStartRide(username, bikeName, bikeType)
        .thenAccept(
            v -> {
              logger.info("Start ride notification processed successfully");
              metricsManager.recordTimer(timer, "notifyStartRide");
            })
        .exceptionally(
            ex -> {
              logger.error("Error processing start ride notification: {}", ex.getMessage());
              metricsManager.recordError(timer, "notifyStartRide", ex);
              return null;
            });
  }

  private void notifyStopRide(String username, String bikeName, BikeType bikeType) {
    metricsManager.incrementMethodCounter("notifyStopRide");
    var timer = metricsManager.startTimer();

    logger.info("Processing stop ride notification for user: {} and bike: {}", username, bikeName);

    bikeMapService
        .notifyStopRide(username, bikeName, bikeType)
        .thenAccept(
            v -> {
              logger.info("Stop ride notification processed successfully");
              metricsManager.recordTimer(timer, "notifyStopRide");
            })
        .exceptionally(
            ex -> {
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
