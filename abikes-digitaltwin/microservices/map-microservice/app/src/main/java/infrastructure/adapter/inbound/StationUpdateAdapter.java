package infrastructure.adapter.inbound;

import application.ports.StationMapServiceAPI;
import domain.model.Slot;
import domain.model.Station;
import domain.model.StationFactory;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.proto.StationProtos;
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

public class StationUpdateAdapter {

  private static final Logger logger = LoggerFactory.getLogger(StationUpdateAdapter.class);
  private final StationMapServiceAPI mapService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;

  public StationUpdateAdapter(StationMapServiceAPI mapService, KafkaProperties kafkaProperties) {
    this.mapService = mapService;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, byte[]> consumer =
            new KafkaConsumer<>(kafkaProperties.getProtobufConsumerProperties());

    try (consumer) {
      consumer.subscribe(List.of(Topics.STATION_UPDATES.getTopicName()));

      while (running.get()) {
        try {
          ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, byte[]> record : records) {
            try {
              StationProtos.Station stationProto = StationProtos.Station.parseFrom(record.value());
              Station station = createStationFromProto(stationProto);
              mapService
                      .updateStation(station)
                      .thenAccept(v -> logger.info("Station {} updated successfully", station.getId()))
                      .exceptionally(
                              ex -> {
                                logger.error(
                                        "Failed to update Station {}: {}", station.getId(), ex.getMessage());
                                return null;
                              });
            } catch (Exception e) {
              logger.error("Invalid Station data from Kafka: {}", e.getMessage());
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

  private Station createStationFromProto(StationProtos.Station stationProto) {
    String stationId = stationProto.getId();
    StationProtos.Location location = stationProto.getLocation();
    float x = (float) location.getX();
    float y = (float) location.getY();

    List<Slot> slots = stationProto.getSlotsList().stream()
            .map(slotProto -> {
              String slotId = slotProto.getId();
              String abikeId = slotProto.getAbikeId().isEmpty() ? null : slotProto.getAbikeId();
              return new Slot(slotId, abikeId);
            })
            .toList();

    int maxSlots = stationProto.getMaxSlots();

    StationFactory factory = StationFactory.getInstance();
    return factory.createStation(stationId, x, y, slots, maxSlots);
  }
}