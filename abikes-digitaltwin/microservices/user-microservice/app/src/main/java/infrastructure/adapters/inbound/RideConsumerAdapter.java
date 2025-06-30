package infrastructure.adapters.inbound;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_BIKE_DISPATCH;
import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
import domain.events.BikeDispatch;
import domain.events.RideUserUpdate;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
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

public class RideConsumerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideConsumerAdapter.class);
  private final UserServiceAPI userService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;
  private final Vertx vertx;

  public RideConsumerAdapter(
          UserServiceAPI userService, Vertx vertx, KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
    this.userService = userService;
    this.vertx = vertx;
  }

  public void init() {
    startKafkaConsumer();
  }

  private void startKafkaConsumer() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    try (KafkaConsumer<String, Object> consumer =
                 new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties())) {

      List<String> topicsToSubscribe =
              List.of(RIDE_USER_UPDATE.getTopicName(), RIDE_BIKE_DISPATCH.getTopicName());
      consumer.subscribe(topicsToSubscribe);
      logger.info("Subscribed to Kafka topic: {}", topicsToSubscribe);

      while (running.get()) {
        try {
          ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, Object> record : records) {
            try {
              if (record.topic().equals(RIDE_USER_UPDATE.getTopicName())) {
                if (record.value() instanceof RideUserUpdate userUpdate) {
                  processUserUpdate(userUpdate);
                }
              } else if (record.topic().equals(RIDE_BIKE_DISPATCH.getTopicName())) {
                if (record.value() instanceof BikeDispatch bikeDispatch) {
                  processBikeDispatch(record.key(), bikeDispatch);
                }
              }
            } catch (Exception e) {
              logger.error("Error processing Avro message: {}", e.getMessage(), e);
            }
          }
          consumer.commitAsync((offsets, exception) -> {
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

  private void processUserUpdate(RideUserUpdate userUpdate) {
    try {
      String username = userUpdate.getUsername();
      int credit = userUpdate.getCredit();

      logger.info("Received Avro user update: username={}, credit={}", username, credit);

      userService
              .updateCredit(username, credit)
              .thenAccept(
                      updatedUser -> {
                        if (updatedUser != null) {
                          logger.info("User credit updated via Kafka: {}", updatedUser);
                        } else {
                          logger.error("User not found via Kafka update: {}", username);
                        }
                      })
              .exceptionally(
                      e -> {
                        logger.error("Error processing user credit update from Kafka", e);
                        return null;
                      });
    } catch (Exception e) {
      logger.error("Failed to process Avro user update: {}", e.getMessage(), e);
    }
  }

  private void processBikeDispatch(String username, BikeDispatch bikeDispatch) {
    logger.info("Dispatching bike to user: {}", bikeDispatch);
    if (username != null && !username.isBlank()) {
      vertx.eventBus().publish(username, bikeDispatch);
    } else {
      logger.error("Dispatch message missing username: {}", bikeDispatch);
    }
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideConsumerAdapter Kafka consumer executor shut down.");
  }
}