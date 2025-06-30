package infrastructure.adapters.inbound;

import application.ports.ABikeServiceAPI;
import application.ports.StationServiceAPI;
import domain.model.ABike;
import domain.model.ABikeMapper;
import domain.events.BikeRideUpdate;
import domain.events.RideUpdate;
import domain.events.BikeActionUpdate;
import domain.events.BikeStationUpdate;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
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
            KafkaConsumer<String, RideUpdate> rideUpdateAvroConsumer =
                    new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties())
    ) {
      avroConsumer.subscribe(List.of(Topics.ABIKE_RIDE_UPDATE.getTopicName()));
      rideUpdateAvroConsumer.subscribe(List.of(Topics.RIDE_UPDATE.getTopicName()));

      while (running.get()) {
        // Handle ABike Avro updates
        ConsumerRecords<String, BikeRideUpdate> avroRecords =
                avroConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, BikeRideUpdate> record : avroRecords) {
          processABikeRideUpdate(record.value());
        }
        avroConsumer.commitAsync();

        // Handle RideUpdate Avro messages
        ConsumerRecords<String, RideUpdate> rideUpdateRecords =
                rideUpdateAvroConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, RideUpdate> record : rideUpdateRecords) {
          processRideUpdate(record.value());
        }
        rideUpdateAvroConsumer.commitAsync();
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

  private void processRideUpdate(RideUpdate rideUpdate) {
    if (rideUpdate == null) return;
    Object payload = rideUpdate.getPayload();
    if (payload instanceof BikeActionUpdate actionUpdate) {
      String bikeId = actionUpdate.getBikeName();
      String action = actionUpdate.getAction();
      if (bikeId == null) {
        logger.error("Incomplete BikeActionUpdate data: {}", actionUpdate);
        return;
      }
      if ("start".equals(action)) {
        stationService.deassignBikeFromStation(bikeId);
      }
    } else if (payload instanceof BikeStationUpdate stationUpdate) {
      String bikeId = stationUpdate.getBikeName();
      String stationId = stationUpdate.getStationId();
      if (bikeId == null || stationId == null) {
        logger.error("Incomplete BikeStationUpdate data: {}", stationUpdate);
        return;
      }
      stationService.assignBikeToStation(stationId, bikeId);
    } else {
      logger.error("Unknown payload type in RideUpdate: {}", payload != null ? payload.getClass() : "null");
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