package infrastructure.adapter.inbound;

import domain.model.P2d;
import domain.model.Slot;
import domain.model.Station;
import domain.model.StationFactory;
import domain.model.repository.StationRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.proto.StationProtos;
import infrastructure.utils.KafkaProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationConsumerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(StationConsumerAdapter.class);
  private final StationRepository stationRepository;
  private final StationFactory stationFactory = StationFactory.getInstance();
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;

  public StationConsumerAdapter(
          StationRepository stationRepository, KafkaProperties kafkaProperties) {
    this.stationRepository = stationRepository;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    startKafkaConsumer();
    logger.info("StationConsumerAdapter initialized");
  }

  private void startKafkaConsumer() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    try (KafkaConsumer<String, byte[]> consumer =
                 new KafkaConsumer<>(kafkaProperties.getProtobufConsumerProperties())) {
      consumer.subscribe(Collections.singletonList(Topics.STATION_UPDATES.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", Topics.STATION_UPDATES.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, byte[]> record : records) {
            try {
              StationProtos.Station stationProto = StationProtos.Station.parseFrom(record.value());
              processStationUpdate(stationProto);
            } catch (Exception e) {
              logger.error("Error processing station update from Kafka: {}", e.getMessage(), e);
            }
          }

          consumer.commitAsync(
                  (offsets, exception) -> {
                    if (exception != null) {
                      logger.error("Failed to commit offsets: {}", exception.getMessage());
                    }
                  });
        } catch (Exception e) {
          logger.error("Error during Kafka polling: {}", e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      logger.error("Error setting up Kafka consumer: {}", e.getMessage(), e);
    }
  }

  private void processStationUpdate(StationProtos.Station stationProto) {
    try {
      String id = stationProto.getId();
      logger.info("Received Station update: id={}", id);

      Station domainStation = convertProtoToDomain(stationProto);

      stationRepository
              .findById(id)
              .thenAccept(
                      existingStation -> {
                        if (existingStation.isPresent()) {
                          stationRepository
                                  .update(domainStation)
                                  .exceptionally(
                                          ex -> {
                                            logger.error("Failed to update Station: {}", ex.getMessage(), ex);
                                            return null;
                                          });
                        } else {
                          stationRepository
                                  .save(domainStation)
                                  .exceptionally(
                                          ex -> {
                                            logger.error("Failed to save new Station: {}", ex.getMessage(), ex);
                                            return null;
                                          });
                        }
                      })
              .exceptionally(
                      ex -> {
                        logger.error("Failed to check if Station exists: {}", ex.getMessage(), ex);
                        return null;
                      });
    } catch (Exception e) {
      logger.error("Failed to process Station update: {}", e.getMessage(), e);
    }
  }

  private Station convertProtoToDomain(StationProtos.Station stationProto) {
    String id = stationProto.getId();

    StationProtos.Location locationProto = stationProto.getLocation();
    double x = locationProto.getX();
    double y = locationProto.getY();

    List<Slot> slots = new ArrayList<>();
    for (StationProtos.Slot slotProto : stationProto.getSlotsList()) {
      String slotId = slotProto.getId();
      String abikeId = slotProto.getAbikeId().isEmpty() ? null : slotProto.getAbikeId();
      slots.add(new Slot(slotId, abikeId));
    }

    int maxSlots = stationProto.getMaxSlots();

    return stationFactory.createStation(id, x, y, slots, maxSlots);
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("StationConsumerAdapter Kafka consumer executor shut down");
  }
}