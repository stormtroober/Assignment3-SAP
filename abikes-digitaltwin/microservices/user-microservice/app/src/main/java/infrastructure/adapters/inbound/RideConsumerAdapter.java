package infrastructure.adapters.inbound;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_BIKE_DISPATCH;
import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
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
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaProperties.getConsumerProperties());

    try (consumer) {
      List<String> topicsToSubscribe =
          List.of(RIDE_USER_UPDATE.getTopicName(), RIDE_BIKE_DISPATCH.getTopicName());
      consumer.subscribe(topicsToSubscribe);
      logger.info("Subscribed to Kafka topic: {}", topicsToSubscribe);

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject message = new JsonObject(record.value());
              if (record.topic().equals(Topics.RIDE_BIKE_DISPATCH.getTopicName())) {
                String userId = record.key();
                logger.info("Received dispatch message: {}", message);
                processBikeDispatch(userId, message);
              } else if (record.topic().equals(RIDE_USER_UPDATE.getTopicName())) {
                logger.info("Received user update from Kafka: {}", message);
                processUserUpdate(message);
              }
            } catch (Exception e) {
              logger.error("Invalid user data from Kafka: {}", e.getMessage());
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

  private void processUserUpdate(JsonObject user) {
    try {
      String username = user.getString("username");
      Integer credit = user.getInteger("credit");

      if (username == null || credit == null) {
        logger.error("Invalid user data from Kafka - missing username or credit: {}", user);
        return;
      }

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
      logger.error("Invalid JSON format in Kafka message", e);
    }
  }

  private void processBikeDispatch(String username, JsonObject message) {
    logger.info("Dispatching bike to user: {}", message);
    if (username != null && !username.isBlank()) {
      vertx.eventBus().publish(username, message.encode());
    } else {
      logger.error("Dispatch message missing username: {}", message);
    }
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideCommunicationAdapter Kafka consumer executor shut down.");
  }
}
