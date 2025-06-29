package infrastructure.adapters.inbound;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_BIKE_DISPATCH;
import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
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
import ride.Ride.RideUserUpdate;
import ride.Ride.BikeDispatch;

public class RideConsumerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideConsumerAdapter.class);
  private final UserServiceAPI userService;
  private final KafkaProperties kafkaProperties;
  private final Vertx vertx;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public RideConsumerAdapter(
          UserServiceAPI userService, Vertx vertx, KafkaProperties kafkaProperties) {
    this.userService = userService;
    this.vertx = vertx;
    this.kafkaProperties = kafkaProperties;
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
    KafkaConsumer<String, byte[]> consumer =
            new KafkaConsumer<>(kafkaProperties.getConsumerProperties());

    try (consumer) {
      List<String> topicsToSubscribe = List.of(
              RIDE_USER_UPDATE.getTopicName(),
              RIDE_BIKE_DISPATCH.getTopicName()
      );
      consumer.subscribe(topicsToSubscribe);
      logger.info("Subscribed to Kafka topics: {}", topicsToSubscribe);

      while (running.get()) {
        try {
          ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, byte[]> record : records) {
            try {
              if (record.topic().equals(RIDE_BIKE_DISPATCH.getTopicName())) {
                BikeDispatch bikeDispatch = BikeDispatch.parseFrom(record.value());
                String username = record.key();
                logger.info("Received BikeDispatch: {}", bikeDispatch);
                processBikeDispatch(username, bikeDispatch);
              } else if (record.topic().equals(RIDE_USER_UPDATE.getTopicName())) {
                RideUserUpdate userUpdate = RideUserUpdate.parseFrom(record.value());
                logger.info("Received UserUpdate: {}", userUpdate);
                processUserUpdate(userUpdate);
              }
            } catch (Exception e) {
              logger.error("Error parsing Protobuf message: {}", e.getMessage(), e);
            }
          }
          consumer.commitAsync((offsets, ex) -> {
            if (ex != null) {
              logger.error("Failed to commit offsets: {}", ex.getMessage(), ex);
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

  private void processUserUpdate(RideUserUpdate user) {
    try {
      String username = user.getUsername();
      int credit = user.getCredit();

      if (username.isBlank()) {
        logger.error("UserUpdate missing username: {}", user);
        return;
      }

      userService.updateCredit(username, credit)
              .thenAccept(updatedUser -> {
                if (updatedUser != null) {
                  logger.info("User credit updated from Kafka: {}", updatedUser);
                } else {
                  logger.error("User not found during Kafka update: {}", username);
                }
              })
              .exceptionally(ex -> {
                logger.error("Error updating user credit", ex);
                return null;
              });
    } catch (Exception e) {
      logger.error("Exception while processing UserUpdate", e);
    }
  }

  private void processBikeDispatch(String username, BikeDispatch bikeDispatch) {
    logger.info("Dispatching bike to user via EventBus: {}", bikeDispatch);
    if (username != null && !username.isBlank()) {
      io.vertx.core.json.JsonObject jsonMessage = new io.vertx.core.json.JsonObject()
              .put("positionX", bikeDispatch.getPositionX())
              .put("positionY", bikeDispatch.getPositionY())
              .put("bikeId", bikeDispatch.getBikeId())
              .put("status", bikeDispatch.getStatus());

      vertx.eventBus().publish(username, jsonMessage);
    } else {
      logger.error("BikeDispatch missing username: {}", bikeDispatch);
    }
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideConsumerAdapter Kafka consumer shut down.");
  }
}
