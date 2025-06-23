package infrastructure.adapter.inbound;

import domain.model.ABikeMapper;
import domain.model.EBikeMapper;
import domain.model.repository.ABikeRepository;
import domain.model.repository.EBikeRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BikeConsumerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(BikeConsumerAdapter.class);
  private final ABikeRepository abikeRepository;
  private final EBikeRepository ebikeRepository;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private KafkaProperties kafkaProperties;

  public BikeConsumerAdapter(
      ABikeRepository abikeRepository,
      EBikeRepository ebikeRepository,
      KafkaProperties kafkaProperties) {
    this.abikeRepository = abikeRepository;
    this.ebikeRepository = ebikeRepository;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    startKafkaConsumer();
    logger.info("BikeConsumerAdapter initialized");
  }

  private void startKafkaConsumer() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaProperties.getConsumerProperties())) {
      consumer.subscribe(
          Arrays.asList(Topics.ABIKE_UPDATES.getTopicName(), Topics.EBIKE_UPDATES.getTopicName()));
      logger.info(
          "Subscribed to Kafka topics: {} and {}",
          Topics.ABIKE_UPDATES.getTopicName(),
          Topics.EBIKE_UPDATES.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject bikeJson = new JsonObject(record.value());
              if (record.topic().equals(Topics.ABIKE_UPDATES.getTopicName())) {
                processABikeUpdate(bikeJson);
              } else if (record.topic().equals(Topics.EBIKE_UPDATES.getTopicName())) {
                processEBikeUpdate(bikeJson);
              }
            } catch (Exception e) {
              logger.error("Error processing bike update from Kafka: {}", e.getMessage(), e);
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

  private void processABikeUpdate(JsonObject abikeJson) {
    try {
      String id = abikeJson.getString("id");
      // logger.info("Received ABike update: id={}", id);

      abikeRepository
          .findById(id)
          .thenAccept(
              existingBike -> {
                if (existingBike.isPresent()) {
                  abikeRepository
                      .update(ABikeMapper.fromJson(abikeJson))
                      .exceptionally(
                          ex -> {
                            logger.error("Failed to update ABike: {}", ex.getMessage(), ex);
                            return null;
                          });
                } else {
                  abikeRepository
                      .save(ABikeMapper.fromJson(abikeJson))
                      .exceptionally(
                          ex -> {
                            logger.error("Failed to save new ABike: {}", ex.getMessage(), ex);
                            return null;
                          });
                }
              })
          .exceptionally(
              ex -> {
                logger.error("Failed to check if ABike exists: {}", ex.getMessage(), ex);
                return null;
              });
    } catch (Exception e) {
      logger.error("Failed to process ABike update: {}", e.getMessage(), e);
    }
  }

  private void processEBikeUpdate(JsonObject ebikeJson) {
    try {
      String id = ebikeJson.getString("id");
      logger.info("Received EBike update: id={}", id);

      ebikeRepository
          .findById(id)
          .thenAccept(
              existingBike -> {
                if (existingBike.isPresent()) {
                  ebikeRepository
                      .update(EBikeMapper.fromJson(ebikeJson))
                      .exceptionally(
                          ex -> {
                            logger.error("Failed to update EBike: {}", ex.getMessage(), ex);
                            return null;
                          });
                } else {
                  ebikeRepository
                      .save(EBikeMapper.fromJson(ebikeJson))
                      .exceptionally(
                          ex -> {
                            logger.error("Failed to save new EBike: {}", ex.getMessage(), ex);
                            return null;
                          });
                }
              })
          .exceptionally(
              ex -> {
                logger.error("Failed to check if EBike exists: {}", ex.getMessage(), ex);
                return null;
              });
    } catch (Exception e) {
      logger.error("Failed to process EBike update: {}", e.getMessage(), e);
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
