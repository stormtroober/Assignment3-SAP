package infrastructure.adapter.station;

import application.ports.BikeMapServiceAPI;
import application.ports.StationMapServiceAPI;
import domain.model.Station;
import domain.model.StationFactory;
import infrastructure.adapter.kafkatopic.Topics;
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

public class StationUpdateAdapter {

  private static final Logger logger = LoggerFactory.getLogger(StationUpdateAdapter.class);
  private final StationMapServiceAPI mapService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public StationUpdateAdapter(StationMapServiceAPI mapService) {
    this.mapService = mapService;
  }

  public void init() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

    try (consumer) {
      consumer.subscribe(List.of(Topics.STATION_UPDATES.getTopicName()));

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject body = new JsonObject(record.value());
              Station station = createStationFromJson(body);
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

  private Station createStationFromJson(JsonObject body) {
    String stationId = body.getString("id");
    JsonObject location = body.getJsonObject("location");
    double x = location.getDouble("x");
    double y = location.getDouble("y");

    StationFactory factory = StationFactory.getInstance();
    return factory.createStation(stationId, (float) x, (float) y);
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("StationUpdateAdapter stopped and Kafka consumer executor shut down.");
  }
}