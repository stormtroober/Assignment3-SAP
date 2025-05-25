package infrastructure.adapter.station;

import domain.model.repository.StationRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.Collections;
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
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public StationConsumerAdapter(StationRepository stationRepository) {
    this.stationRepository = stationRepository;
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
    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties())) {
      consumer.subscribe(Collections.singletonList(Topics.STATION_UPDATES.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", Topics.STATION_UPDATES.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject stationJson = new JsonObject(record.value());
              processStationUpdate(stationJson);
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

  private void processStationUpdate(JsonObject stationJson) {
    try {
      String id = stationJson.getString("id");
      logger.info("Received Station update: id={}", id);

      stationRepository
          .findById(id)
          .thenAccept(
              existingStation -> {
                if (existingStation.isPresent()) {
                  stationRepository
                      .update(stationJson)
                      .exceptionally(
                          ex -> {
                            logger.error("Failed to update Station: {}", ex.getMessage(), ex);
                            return null;
                          });
                } else {
                  stationRepository
                      .save(stationJson)
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

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("StationConsumerAdapter Kafka consumer executor shut down");
  }
}