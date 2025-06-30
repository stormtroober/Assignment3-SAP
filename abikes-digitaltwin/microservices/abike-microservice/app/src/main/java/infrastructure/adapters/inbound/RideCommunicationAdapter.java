package infrastructure.adapters.inbound;

import application.ports.ABikeServiceAPI;
import application.ports.StationServiceAPI;
import domain.model.ABike;
import domain.model.ABikeMapper;
import domain.events.BikeRideUpdate;
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
  private final KafkaProperties kafkaProperties;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public RideCommunicationAdapter(
          ABikeServiceAPI aBikeService,
          StationServiceAPI stationService,
          KafkaProperties kafkaProperties) {
    this.aBikeService = aBikeService;
    this.stationService = stationService;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    logger.info("Initializing Kafka consumers for ride updates");
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumers);
  }

  private void runKafkaConsumers() {
    try (
            KafkaConsumer<String, BikeRideUpdate> avroConsumer =
                    new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties());
            KafkaConsumer<String, String> stringConsumer =
                    new KafkaConsumer<>(kafkaProperties.getConsumerProperties())
    ) {
      avroConsumer.subscribe(List.of(Topics.ABIKE_RIDE_UPDATE.getTopicName()));
      stringConsumer.subscribe(List.of(Topics.RIDE_UPDATE.getTopicName()));

      while (running.get()) {
        // Handle Avro updates
        ConsumerRecords<String, BikeRideUpdate> avroRecords =
                avroConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, BikeRideUpdate> record : avroRecords) {
          processABikeRideUpdate(record.value());
        }
        avroConsumer.commitAsync();

        // Handle String (JSON) updates
        ConsumerRecords<String, String> stringRecords =
                stringConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : stringRecords) {
          try {
            processRideUpdate(new JsonObject(record.value()));
          } catch (Exception e) {
            logger.error("Failed to parse RIDE_UPDATE message: {}", e.getMessage());
          }
        }
        stringConsumer.commitAsync();
      }
    } catch (Exception e) {
      logger.error("Error in Kafka consumer loop: {}", e.getMessage(), e);
    }
  }

  private void processABikeRideUpdate(BikeRideUpdate update) {
    try {
      ABike aBike = ABikeMapper.fromAvro(update);
      aBikeService
              .updateABike(aBike)
              .thenAccept(v -> logger.info("ABike {} updated successfully via Avro Kafka consumer", aBike.getId()))
              .exceptionally(e -> {
                logger.error("Failed to update ABike {}: {}", aBike.getId(), e.getMessage());
                return null;
              });
    } catch (Exception e) {
      logger.error("Invalid ABike Avro data from Kafka: {}", e.getMessage());
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
      if ("start".equals(action)) {
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
    logger.info("RideCommunicationAdapter Kafka consumer executor shut down");
  }
}