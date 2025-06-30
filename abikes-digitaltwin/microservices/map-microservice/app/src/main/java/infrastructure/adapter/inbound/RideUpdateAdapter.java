package infrastructure.adapter.inbound;

import static infrastructure.adapter.kafkatopic.Topics.RIDE_UPDATE;

import application.ports.BikeMapServiceAPI;
import domain.events.BikeActionUpdate;
import domain.events.BikeStationUpdate;
import domain.events.RideUpdate;
import domain.model.BikeType;
import infrastructure.utils.KafkaProperties;
import infrastructure.utils.MetricsManager;
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
  private final KafkaProperties kafkaProperties;

  public RideUpdateAdapter(BikeMapServiceAPI bikeMapService, KafkaProperties kafkaProperties) {
    this.bikeMapService = bikeMapService;
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
    KafkaConsumer<String, RideUpdate> consumer =
            new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties());

    try (consumer) {
      consumer.subscribe(List.of(RIDE_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", RIDE_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, RideUpdate> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, RideUpdate> record : records) {
            try {
              RideUpdate rideUpdate = record.value();
              if (rideUpdate == null) continue;
              processRideUpdate(rideUpdate);
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

  private void processRideUpdate(RideUpdate rideUpdate) {
    Object payload = rideUpdate.getPayload();
    if (payload instanceof BikeActionUpdate actionUpdate) {
      processBikeActionUpdate(actionUpdate);
    } else if (payload instanceof BikeStationUpdate stationUpdate) {
      logger.info("Received BikeStationUpdate: bikeName={}, stationId={}",
              stationUpdate.getBikeName(), stationUpdate.getStationId());
      // Add handling if needed
    } else {
      logger.error("Unknown payload type in RideUpdate: {}", payload != null ? payload.getClass() : "null");
    }
  }

  private void processBikeActionUpdate(BikeActionUpdate actionUpdate) {
    String action = actionUpdate.getAction();
    String username = actionUpdate.getUsername();
    String bikeName = actionUpdate.getBikeName();
    String bikeTypeStr = actionUpdate.getBikeType();

    if (action == null || bikeName == null || bikeTypeStr == null) {
      logger.error("Incomplete BikeActionUpdate data: {}", actionUpdate);
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
      case "public_start":
        notifyStartPublicRide(bikeName, bikeType);
        break;
      case "public_end":
        notifyStopPublicRide(bikeName, bikeType);
        break;
      default:
        logger.error("Unknown action in BikeActionUpdate: {}", action);
    }
  }

  private void notifyStartPublicRide(String bikeName, BikeType bikeType) {
    metricsManager.incrementMethodCounter("notifyStartPublicRide");
    var timer = metricsManager.startTimer();

    logger.info("Processing public start ride notification for bike: {}", bikeName);

    bikeMapService
            .notifyStartPublicRide(bikeName, bikeType)
            .thenAccept(
                    v -> {
                      logger.info("Public start ride notification processed successfully");
                      metricsManager.recordTimer(timer, "notifyStartPublicRide");
                    })
            .exceptionally(
                    ex -> {
                      logger.error("Error processing public start ride notification: {}", ex.getMessage());
                      metricsManager.recordError(timer, "notifyStartPublicRide", ex);
                      return null;
                    });
  }

  private void notifyStopPublicRide(String bikeName, BikeType bikeType) {
    metricsManager.incrementMethodCounter("notifyStopPublicRide");
    var timer = metricsManager.startTimer();

    logger.info("Processing public stop ride notification for bike: {}", bikeName);

    bikeMapService
            .notifyStopPublicRide(bikeName, bikeType)
            .thenAccept(
                    v -> {
                      logger.info("Public stop ride notification processed successfully");
                      metricsManager.recordTimer(timer, "notifyStopPublicRide");
                    })
            .exceptionally(
                    ex -> {
                      logger.error("Error processing public stop ride notification: {}", ex.getMessage());
                      metricsManager.recordError(timer, "notifyStopPublicRide", ex);
                      return null;
                    });
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