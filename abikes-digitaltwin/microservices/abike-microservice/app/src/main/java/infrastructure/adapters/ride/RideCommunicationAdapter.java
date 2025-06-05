package infrastructure.adapters.ride;

import application.ports.ABikeServiceAPI;
import application.ports.StationServiceAPI;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideCommunicationAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
  private final ABikeServiceAPI aBikeService;
  private final StationServiceAPI stationService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
    private final KafkaProperties kafkaProperties;

  public RideCommunicationAdapter(ABikeServiceAPI aBikeService, StationServiceAPI stationService, KafkaProperties kafkaProperties) {
    this.aBikeService = aBikeService;
    this.stationService = stationService;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    initKafkaConsumer();
  }

  private void initKafkaConsumer() {
    logger.info("Initializing Kafka consumer for Ride updates");
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaProperties.getConsumerProperties());
    try (consumer) {
      consumer.subscribe(
          List.of(Topics.ABIKE_RIDE_UPDATE.getTopicName(), Topics.RIDE_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topics for ride updates");

      while (running.get()) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          if (record.topic().equals(Topics.RIDE_UPDATE.getTopicName())) {
            logger.info("Received ride update from Kafka: {}", record.value());
            processRideUpdate(new JsonObject(record.value()));
          } else if (record.topic().equals(Topics.ABIKE_RIDE_UPDATE.getTopicName())) {
            processABikeRideUpdate(new JsonObject(record.value()));
          }
        }
        consumer.commitAsync(
            (offsets, exception) -> {
              if (exception != null) {
                logger.error("Failed to commit offsets: {}", exception.getMessage());
              }
            });
      }
    } catch (Exception e) {
      logger.error("Error in Kafka consumer loop: {}", e.getMessage(), e);
    }
  }

  private void processABikeRideUpdate(JsonObject updateJson) {
    try {
      String id = updateJson.getString("id");
      aBikeService
          .updateABike(updateJson)
          .thenAccept(v -> logger.info("ABike {} updated successfully via Kafka consumer", id))
          .exceptionally(
              e -> {
                logger.error("Failed to update ABike {}: {}", id, e.getMessage());
                return null;
              });
    } catch (Exception e) {
      logger.error("Invalid ABike data from Kafka: {}", e.getMessage());
    }
  }

  private void processRideUpdate(JsonObject rideUpdate) {
    String bikeId = rideUpdate.getString("bikeName");

    if (bikeId == null) {
      logger.error("Incomplete ride update data: {}", rideUpdate);
      return;
    }

    if (rideUpdate.containsKey("action")) {
      String action = rideUpdate.getString("action");
      if (action.equals("start")) {
        stationService.deassignBikeFromStation(bikeId);
      }
    }

    if (rideUpdate.containsKey("stationId")) {
      String stationId = rideUpdate.getString("stationId");
      stationService.assignBikeToStation(stationId, bikeId);
    }
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("BikeConsumerAdapter Kafka consumer executor shut down");
  }
}
